package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.WatchlistItemRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import co.edu.unbosque.acciones_elbosque.identity_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class WatchlistControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withInitScript("test-init.sql");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired WatchlistItemRepository watchlistItemRepository;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean AuditService auditService;

    private String premiumToken;
    private String basicToken;

    @BeforeEach
    void setUp() {
        watchlistItemRepository.deleteAll();
        userRepository.deleteAll();

        User premiumUser = User.builder()
                .email("premium@test.com").passwordHash("hash")
                .firstName("Ana").lastName("Lopez")
                .phone("+57300000001").birthDate(LocalDate.of(1990, 1, 1))
                .docType("cc").docNumber("11111111")
                .nationality("Colombiana").country("CO")
                .address("Calle 1").city("Bogotá").postalCode("110111")
                .incomeSource("salary").annualIncome("30k-100k")
                .netWorth("50k-250k").riskTolerance("moderate")
                .tradingExperience("beginner").investmentGoal("growth")
                .role("INVESTOR").plan("PREMIUM").build();

        User basicUser = User.builder()
                .email("basic@test.com").passwordHash("hash")
                .firstName("Luis").lastName("Perez")
                .phone("+57300000002").birthDate(LocalDate.of(1992, 1, 1))
                .docType("cc").docNumber("22222222")
                .nationality("Colombiano").country("CO")
                .address("Calle 2").city("Medellín").postalCode("050001")
                .incomeSource("salary").annualIncome("30k-100k")
                .netWorth("50k-250k").riskTolerance("moderate")
                .tradingExperience("beginner").investmentGoal("growth")
                .role("INVESTOR").plan("BASIC").build();

        premiumToken = jwtUtil.generateToken(userRepository.save(premiumUser));
        basicToken   = jwtUtil.generateToken(userRepository.save(basicUser));
    }

    @Test
    void getWatchlist_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/watchlist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getWatchlist_basicPlan_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/watchlist")
                        .header("Authorization", "Bearer " + basicToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void addSymbol_premium_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/watchlist/AAPL")
                        .header("Authorization", "Bearer " + premiumToken))
                .andExpect(status().isCreated());
    }

    @Test
    void removeSymbol_premium_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/watchlist/AAPL")
                        .header("Authorization", "Bearer " + premiumToken));

        mockMvc.perform(delete("/api/v1/watchlist/AAPL")
                        .header("Authorization", "Bearer " + premiumToken))
                .andExpect(status().isNoContent());
    }
}
