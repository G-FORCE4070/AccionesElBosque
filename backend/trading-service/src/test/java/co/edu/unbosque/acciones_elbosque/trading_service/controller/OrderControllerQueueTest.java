package co.edu.unbosque.acciones_elbosque.trading_service.controller;

import co.edu.unbosque.acciones_elbosque.trading_service.client.AlpacaBrokerClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.AuditClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.MarketsServiceClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.NotificationClient;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.MarketStatusDto;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.StockPriceDto;
import co.edu.unbosque.acciones_elbosque.trading_service.service.AlpacaTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderControllerQueueTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withInitScript("test-init.sql");

    @Autowired MockMvc mockMvc;

    @MockitoBean AlpacaTokenService alpacaTokenService;
    @MockitoBean MarketsServiceClient marketsClient;
    @MockitoBean AlpacaBrokerClient alpacaClient;
    @MockitoBean AuditClient auditClient;
    @MockitoBean NotificationClient notificationClient;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = generateToken(UUID.randomUUID().toString(), "alpaca-acc-test");

        MarketStatusDto closed = new MarketStatusDto();
        setField(closed, "open", false);
        given(marketsClient.getMarketStatus(anyString(), anyString())).willReturn(closed);

        StockPriceDto price = new StockPriceDto();
        setField(price, "price", new BigDecimal("150.00"));
        given(marketsClient.getStockPrice(anyString(), anyString())).willReturn(price);

        doNothing().when(auditClient).sendEvent(any());
        doNothing().when(notificationClient).notifyOrderExecuted(any());
    }

    @Test
    void buy_queueTrue_marketClosed_returns202() throws Exception {
        mockMvc.perform(post("/api/v1/orders/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"AAPL","quantity":1,"orderType":"market","queue":true}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_QUEUE"))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void buy_noQueue_marketClosed_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/orders/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"AAPL","quantity":1,"orderType":"market"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("El mercado está cerrado"));
    }

    @Test
    void buy_queueTrue_insufficientFunds_returns400() throws Exception {
        StockPriceDto expensive = new StockPriceDto();
        setField(expensive, "price", new BigDecimal("99999.00"));
        given(marketsClient.getStockPrice(anyString(), anyString())).willReturn(expensive);

        mockMvc.perform(post("/api/v1/orders/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"AAPL","quantity":100,"orderType":"market","queue":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fondos insuficientes"));
    }

    @Test
    void cancel_pendingQueueOrder_returns204() throws Exception {
        // Enqueue first, then cancel
        String enqueueResponse = mockMvc.perform(post("/api/v1/orders/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"AAPL","quantity":1,"orderType":"market","queue":true}
                                """))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        String orderId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(enqueueResponse).get("orderId").asText();

        mockMvc.perform(delete("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancel_nonExistentOrder_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Orden no encontrada"));
    }

    @Test
    void cancel_alreadyCancelledOrder_returns409() throws Exception {
        String enqueueResponse = mockMvc.perform(post("/api/v1/orders/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"AAPL","quantity":1,"orderType":"market","queue":true}
                                """))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        String orderId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(enqueueResponse).get("orderId").asText();

        mockMvc.perform(delete("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Solo se pueden cancelar órdenes en estado PENDING_QUEUE"));
    }

    private String generateToken(String userId, String alpacaAccountId) {
        String jwtKey = "default-secret-key-for-dev-must-be-min-32-chars";
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("role", "INVESTOR")
                .claim("alpacaAccountId", alpacaAccountId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }

    private void setField(Object obj, String name, Object value) {
        try {
            var field = findField(obj.getClass(), name);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) return findField(clazz.getSuperclass(), name);
            throw e;
        }
    }
}
