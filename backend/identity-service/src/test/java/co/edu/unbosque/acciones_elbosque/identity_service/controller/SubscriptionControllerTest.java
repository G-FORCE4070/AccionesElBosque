package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserPreferenceRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.StripeCheckoutService;
import co.edu.unbosque.acciones_elbosque.identity_service.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class SubscriptionControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withInitScript("test-init.sql");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired UserPreferenceRepository preferenceRepository;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean AuditService auditService;
    @MockitoBean StripeCheckoutService stripeCheckoutService;

    private String token;

    @BeforeEach
    void setUp() {
        preferenceRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .email("sub-test@test.com")
                .passwordHash("hash")
                .firstName("Carlos")
                .lastName("Ruiz")
                .phone("+57300000000")
                .birthDate(LocalDate.of(1988, 3, 10))
                .docType("cc")
                .docNumber("11223344")
                .nationality("CO")
                .country("CO")
                .address("Carrera 7")
                .city("Bogotá")
                .postalCode("110111")
                .incomeSource("salary")
                .annualIncome("30k-100k")
                .netWorth("50k-250k")
                .riskTolerance("moderate")
                .tradingExperience("beginner")
                .investmentGoal("growth")
                .plan("BASIC")
                .build();
        User saved = userRepository.save(user);
        token = jwtUtil.generateToken(saved);
    }

    @Test
    void checkout_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("billingCycle", "monthly"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkout_validToken_returns200WithCheckoutUrl() throws Exception {
        when(stripeCheckoutService.createCheckoutSession(eq("sub-test@test.com"), eq("monthly")))
                .thenReturn("https://checkout.stripe.com/test-session");

        mockMvc.perform(post("/api/v1/subscriptions/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("billingCycle", "monthly"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/test-session"));
    }

    @Test
    void cancel_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancel_validToken_returns200AndPlanSetToBasic() throws Exception {
        User user = userRepository.findByEmail("sub-test@test.com").orElseThrow();
        user.setPlan("PREMIUM");
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/subscriptions/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User updated = userRepository.findByEmail("sub-test@test.com").orElseThrow();
        assertThat(updated.getPlan()).isEqualTo("BASIC");
    }
}
