package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserPreferenceRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;
import static org.mockito.Mockito.mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class LoginMfaControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withInitScript("test-init.sql");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired UserPreferenceRepository userPreferenceRepository;
    @Autowired BCryptPasswordEncoder passwordEncoder;
    @Autowired CacheManager cacheManager;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean AuditService auditService;

    private static final String EMAIL = "login-test@test.com";
    private static final String PASSWORD = "Password1!";

    @BeforeEach
    void setUp() {
        userPreferenceRepository.deleteAll();
        userRepository.deleteAll();
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        doNothing().when(mailSender).send(any(MimeMessage.class));

        User user = User.builder()
                .email(EMAIL)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .firstName("Test")
                .lastName("User")
                .phone("+57300000000")
                .birthDate(java.time.LocalDate.of(1990, 1, 1))
                .docType("cc")
                .docNumber("12345678")
                .nationality("Colombian")
                .country("COL")
                .address("Calle 1")
                .city("Bogota")
                .postalCode("110111")
                .incomeSource("salary")
                .annualIncome("30k-100k")
                .netWorth("50k-250k")
                .riskTolerance("moderate")
                .tradingExperience("beginner")
                .investmentGoal("growth")
                .pep(false)
                .finraAffiliated(false)
                .acceptsTerms(true)
                .acceptsPrivacy(true)
                .authorizeDataUse(true)
                .role("INVESTOR")
                .plan("BASIC")
                .build();
        userRepository.save(user);

        if (cacheManager.getCache("sessions") != null) cacheManager.getCache("sessions").clear();
        if (cacheManager.getCache("mfaCodes") != null) cacheManager.getCache("mfaCodes").clear();
    }

    @Test
    void login_correctCredentials_returns200WithSessionId() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("sessionId");
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", "WrongPass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Email o contraseña inválidos"));
    }

    @Test
    void login_emptyFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "", "password", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void mfaVerify_correctCode_returns200WithToken() throws Exception {
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(loginBody).get("sessionId").asText();

        // Grab the code directly from cache
        org.springframework.cache.Cache.ValueWrapper wrapper =
                cacheManager.getCache("mfaCodes").get(sessionId);
        co.edu.unbosque.acciones_elbosque.identity_service.model.MfaEntry entry =
                (co.edu.unbosque.acciones_elbosque.identity_service.model.MfaEntry) wrapper.get();

        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", sessionId, "code", entry.getCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    void mfaVerify_wrongCode_returns400() throws Exception {
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(loginBody).get("sessionId").asText();

        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", sessionId, "code", "000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Código inválido"));
    }

    @Test
    void mfaVerify_thirdFailedAttempt_returns400ThenNextReturns404() throws Exception {
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(loginBody).get("sessionId").asText();
        String wrongCodePayload = objectMapper.writeValueAsString(Map.of("sessionId", sessionId, "code", "000000"));

        mockMvc.perform(post("/api/v1/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON).content(wrongCodePayload))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON).content(wrongCodePayload))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON).content(wrongCodePayload))
                .andExpect(status().isBadRequest());

        // 4th attempt — cache evicted, should return 404
        mockMvc.perform(post("/api/v1/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON).content(wrongCodePayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El código ha expirado"));
    }

    @Test
    void mfaResend_validSession_returns200() throws Exception {
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(loginBody).get("sessionId").asText();

        mockMvc.perform(post("/api/v1/auth/mfa/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sessionId", sessionId))))
                .andExpect(status().isOk());
    }
}
