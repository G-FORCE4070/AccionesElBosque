package co.edu.unbosque.acciones_elbosque.trading_service.controller;

import co.edu.unbosque.acciones_elbosque.trading_service.client.AlpacaBrokerClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.AuditClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.MarketsServiceClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.NotificationClient;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Order;
import co.edu.unbosque.acciones_elbosque.trading_service.model.OrderStatus;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.OrderRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.service.AlpacaTokenService;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransactionControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withInitScript("test-init.sql");

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;

    @MockitoBean AlpacaTokenService alpacaTokenService;
    @MockitoBean MarketsServiceClient marketsClient;
    @MockitoBean AlpacaBrokerClient alpacaClient;
    @MockitoBean AuditClient auditClient;
    @MockitoBean NotificationClient notificationClient;

    private String token;
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        token = generateToken(USER_ID.toString(), "alpaca-acc-test");
        orderRepository.deleteAll();

        Order executed = Order.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .symbol("AAPL")
                .side("buy")
                .orderType("market")
                .quantity(3)
                .executedPrice(new BigDecimal("150.00"))
                .commission(new BigDecimal("1.50"))
                .status(OrderStatus.EXECUTED)
                .createdAt(Instant.now())
                .executedAt(Instant.now())
                .build();
        orderRepository.save(executed);

        // pending order — must NOT appear in transactions
        Order pending = Order.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .symbol("TSLA")
                .side("buy")
                .orderType("market")
                .quantity(1)
                .status(OrderStatus.PENDING_QUEUE)
                .createdAt(Instant.now())
                .build();
        orderRepository.save(pending);
    }

    @Test
    void getTransactions_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTransactions_withAuth_returnsOnlyExecuted() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.content[0].type").value("buy"))
                .andExpect(jsonPath("$.totalCommission").value(1.50));
    }

    @Test
    void getTransactions_filterBySymbol_returnsMatching() throws Exception {
        mockMvc.perform(get("/api/v1/transactions?symbol=AAPL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTransactions_filterByUnknownSymbol_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/transactions?symbol=MSFT")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
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
}
