package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserPreferenceRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class UserControllerTest {

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

    private String token;

    @BeforeEach
    void setUp() {
        preferenceRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .email("profile-test@test.com")
                .passwordHash("hash")
                .firstName("Laura")
                .lastName("Torres")
                .phone("+57300000000")
                .birthDate(LocalDate.of(1995, 6, 15))
                .docType("cc")
                .docNumber("99887766")
                .nationality("Colombiana")
                .country("CO")
                .address("Carrera 5")
                .city("Bogotá")
                .postalCode("110111")
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
        token = jwtUtil.generateToken(saved);
    }

    @Test
    void getProfile_validToken_returns200WithProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Laura"))
                .andExpect(jsonPath("$.email").value("profile-test@test.com"))
                .andExpect(jsonPath("$.role").value("INVESTOR"));
    }

    @Test
    void getProfile_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePersonal_validRequest_returns200() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/personal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Laura",
                                "phone", "+57322222222"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void updatePersonal_shortFirstName_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/personal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("firstName", "X"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updatePreferences_validRequest_returns200() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "marketOpen", true,
                                "market", "nasdaq",
                                "sector", "tech"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void updatePreferences_noToken_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("marketOpen", true))))
                .andExpect(status().isUnauthorized());
    }
}
