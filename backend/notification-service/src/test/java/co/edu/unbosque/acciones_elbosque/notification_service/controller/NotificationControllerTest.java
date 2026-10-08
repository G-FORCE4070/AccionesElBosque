package co.edu.unbosque.acciones_elbosque.notification_service.controller;

import co.edu.unbosque.acciones_elbosque.notification_service.client.AuditClient;
import co.edu.unbosque.acciones_elbosque.notification_service.client.IdentityClient;
import co.edu.unbosque.acciones_elbosque.notification_service.dto.UserPreferencesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean IdentityClient identityClient;
    @MockitoBean AuditClient auditClient;

    @Test
    void postNotification_validBody_returns202() throws Exception {
        UserPreferencesResponse prefs = new UserPreferencesResponse();
        prefs.setOrderConfirm(true);
        prefs.setNotifEmail(true);
        given(identityClient.getPreferences(anyString())).willReturn(prefs);
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage((jakarta.mail.Session) null));
        doNothing().when(mailSender).send(any(MimeMessage.class));

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", UUID.randomUUID().toString(),
                "eventType", "ORDER_EXECUTED",
                "data", Map.of(
                        "userEmail", "test@test.com",
                        "symbol", "AAPL",
                        "side", "buy",
                        "quantity", 1,
                        "executedPrice", "150.00",
                        "commission", "3.00",
                        "newBalance", "49847.00"
                )
        ));

        mockMvc.perform(post("/api/v1/internal/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
    }

    @Test
    void postNotification_noAuth_returns202() throws Exception {
        UserPreferencesResponse prefs = new UserPreferencesResponse();
        prefs.setOrderConfirm(false);
        prefs.setNotifEmail(true);
        given(identityClient.getPreferences(anyString())).willReturn(prefs);

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", UUID.randomUUID().toString(),
                "eventType", "ORDER_EXECUTED",
                "data", Map.of("symbol", "TSLA", "side", "sell", "quantity", 2)
        ));

        mockMvc.perform(post("/api/v1/internal/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
    }
}
