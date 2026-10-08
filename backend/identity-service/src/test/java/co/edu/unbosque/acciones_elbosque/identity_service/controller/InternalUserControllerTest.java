package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserPreferenceRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class InternalUserControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withInitScript("test-init.sql");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserPreferenceRepository preferenceRepository;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean AuditService auditService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        preferenceRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .email("internal-test@test.com")
                .passwordHash("hash")
                .firstName("Ana")
                .lastName("Ruiz")
                .phone("+57300000001")
                .birthDate(LocalDate.of(1990, 1, 1))
                .docType("cc")
                .docNumber("11223344")
                .nationality("Colombiana")
                .country("CO")
                .address("Calle 1")
                .city("Bogotá")
                .postalCode("110001")
                .incomeSource("salary")
                .annualIncome("30k-100k")
                .netWorth("50k-250k")
                .riskTolerance("moderate")
                .tradingExperience("beginner")
                .investmentGoal("growth")
                .role("INVESTOR")
                .plan("BASIC")
                .build();
        User saved = userRepository.save(user);
        userId = saved.getId();
    }

    @Test
    void getPreferences_existingUser_returns200WithPreferences() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/{userId}/preferences", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderConfirm").value(true))
                .andExpect(jsonPath("$.notifEmail").value(true));
    }

    @Test
    void getPreferences_unknownUser_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/{userId}/preferences", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
