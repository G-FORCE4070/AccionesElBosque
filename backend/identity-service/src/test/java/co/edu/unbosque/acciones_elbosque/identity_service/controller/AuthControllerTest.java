package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.exception.AlpacaAccountCreationException;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserPreferenceRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AlpacaBrokerService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.StripeCheckoutService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withInitScript("test-init.sql");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AlpacaBrokerService alpacaBrokerService;

    @MockitoBean
    StripeCheckoutService stripeCheckoutService;

    @MockitoBean
    JavaMailSender mailSender;

    @MockitoBean
    AuditService auditService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserPreferenceRepository userPreferenceRepository;

    @BeforeEach
    void setUp() {
        userPreferenceRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> map = new HashMap<>();
        map.put("firstName", "María");
        map.put("lastName", "Clavijo");
        map.put("email", "maria@ejemplo.com");
        map.put("phone", "+57300000000");
        map.put("password", "Password1!");
        map.put("birthDate", "1990-01-01");
        map.put("docType", "cc");
        map.put("docNumber", "12345678");
        map.put("nationality", "Colombia");
        map.put("country", "CO");
        map.put("address", "Calle 123");
        map.put("city", "Bogotá");
        map.put("postalCode", "110111");
        map.put("incomeSource", "salary");
        map.put("annualIncome", "30k-100k");
        map.put("netWorth", "50k-250k");
        map.put("riskTolerance", "moderate");
        map.put("tradingExperience", "beginner");
        map.put("investmentGoal", "growth");
        map.put("isPep", false);
        map.put("isFinraAffiliated", false);
        map.put("acceptsTerms", true);
        map.put("acceptsPrivacy", true);
        map.put("authorizeDataUse", true);
        map.put("markets", List.of("nyse"));
        map.put("sectors", List.of("tech"));
        map.put("plan", "basic");
        return map;
    }

    @Test
    void register_basicPlan_returns201WithNullCheckoutUrl() throws Exception {
        when(alpacaBrokerService.createAccount(any())).thenReturn("alpaca-123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registro exitoso. Revisa tu correo de bienvenida."))
                .andExpect(jsonPath("$.checkoutUrl").isEmpty());
    }

    @Test
    void register_premiumPlan_returns201WithCheckoutUrl() throws Exception {
        when(alpacaBrokerService.createAccount(any())).thenReturn("alpaca-123");
        when(stripeCheckoutService.createCheckoutSession(any(), eq("monthly")))
                .thenReturn("https://checkout.stripe.com/pay/cs_test_123");

        Map<String, Object> payload = validPayload();
        payload.put("plan", "premium");
        payload.put("billingCycle", "monthly");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_123"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(alpacaBrokerService.createAccount(any())).thenReturn("alpaca-123");
        String content = objectMapper.writeValueAsString(validPayload());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El correo electrónico ya está registrado"));
    }

    @Test
    void register_alpacaFails_returns500AndNoUserPersisted() throws Exception {
        doThrow(new AlpacaAccountCreationException("Alpaca error"))
                .when(alpacaBrokerService).createAccount(any());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("No se pudo completar el registro. Intente nuevamente."));

        assertThat(userRepository.existsByEmail("maria@ejemplo.com")).isFalse();
    }

    @Test
    void register_invalidFields_returns400() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("email", "not-valid-email");
        payload.put("password", "weak");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
