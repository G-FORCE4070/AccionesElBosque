package co.edu.unbosque.acciones_elbosque.markets_service.controller;

import co.edu.unbosque.acciones_elbosque.markets_service.model.AlpacaPriceData;
import co.edu.unbosque.acciones_elbosque.markets_service.service.AlpacaClockService;
import co.edu.unbosque.acciones_elbosque.markets_service.service.AlpacaMarketDataService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class StockControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withInitScript("test-init.sql");

    @Autowired MockMvc mockMvc;

    @MockitoBean AlpacaMarketDataService alpacaMarketDataService;
    @MockitoBean AlpacaClockService alpacaClockService;

    private String token;

    @BeforeEach
    void setUp() {
        given(alpacaMarketDataService.getPrice(anyString()))
                .willReturn(new AlpacaPriceData(175.0, 1.2));
        token = generateToken("test-user-id");
    }

    @Test
    void getStocks_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/markets/stocks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStocks_validJwt_returns200WithStocks() throws Exception {
        mockMvc.perform(get("/api/v1/markets/stocks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void getStocks_marketFilter_returnsOnlyNasdaqStocks() throws Exception {
        mockMvc.perform(get("/api/v1/markets/stocks?markets=NASDAQ")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void getMarketStatus_open_returnsOpenTrue() throws Exception {
        given(alpacaClockService.isMarketOpen()).willReturn(true);

        mockMvc.perform(get("/api/v1/markets/status/AAPL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.open").value(true));
    }

    @Test
    void getMarketStatus_closed_returnsOpenFalse() throws Exception {
        given(alpacaClockService.isMarketOpen()).willReturn(false);

        mockMvc.perform(get("/api/v1/markets/status/AAPL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false));
    }

    @Test
    void getMarketStatus_symbolNotFound_returns404() throws Exception {
        given(alpacaClockService.isMarketOpen()).willReturn(true);

        mockMvc.perform(get("/api/v1/markets/status/INVALID")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String generateToken(String userId) {
        String jwtKey = "default-secret-key-for-dev-must-be-min-32-chars";
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("role", "INVESTOR")
                .claim("plan", "BASIC")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }
}
