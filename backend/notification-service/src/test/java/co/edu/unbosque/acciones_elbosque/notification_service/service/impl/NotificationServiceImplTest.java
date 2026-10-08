package co.edu.unbosque.acciones_elbosque.notification_service.service.impl;

import co.edu.unbosque.acciones_elbosque.notification_service.client.AuditClient;
import co.edu.unbosque.acciones_elbosque.notification_service.client.IdentityClient;
import co.edu.unbosque.acciones_elbosque.notification_service.dto.AuditEventRequest;
import co.edu.unbosque.acciones_elbosque.notification_service.dto.NotificationRequest;
import co.edu.unbosque.acciones_elbosque.notification_service.dto.UserPreferencesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock IdentityClient identityClient;
    @Mock AuditClient auditClient;
    @Mock JavaMailSender mailSender;

    NotificationServiceImpl service;

    static final String USER_ID = UUID.randomUUID().toString();
    static final String USER_EMAIL = "investor@test.com";

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(identityClient, auditClient, mailSender);
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@aeb.com");
    }

    @Test
    void processNotification_orderConfirmTrue_sendsEmail() throws Exception {
        given(identityClient.getPreferences(USER_ID)).willReturn(prefsEnabled());
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage((jakarta.mail.Session) null));
        doNothing().when(mailSender).send(any(MimeMessage.class));

        service.processNotification(buildRequest(USER_EMAIL));

        verify(mailSender).send(any(MimeMessage.class));
        verify(auditClient).sendEvent(argThat(e -> "NOTIFICATION_SENT".equals(e.getEventType())));
    }

    @Test
    void processNotification_orderConfirmFalse_skipsEmail() {
        UserPreferencesResponse prefs = new UserPreferencesResponse();
        prefs.setOrderConfirm(false);
        prefs.setNotifEmail(true);
        given(identityClient.getPreferences(USER_ID)).willReturn(prefs);

        service.processNotification(buildRequest(USER_EMAIL));

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(auditClient, never()).sendEvent(any());
    }

    @Test
    void processNotification_identityUnavailable_usesDefaultsAndSendsEmail() throws Exception {
        given(identityClient.getPreferences(USER_ID)).willThrow(new RestClientException("timeout"));
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage((jakarta.mail.Session) null));
        doNothing().when(mailSender).send(any(MimeMessage.class));

        service.processNotification(buildRequest(USER_EMAIL));

        verify(mailSender).send(any(MimeMessage.class));
        verify(auditClient).sendEvent(argThat(e -> "NOTIFICATION_SENT_DEFAULT_PREFS".equals(e.getEventType())));
        verify(auditClient).sendEvent(argThat(e -> "NOTIFICATION_SENT".equals(e.getEventType())));
    }

    @Test
    void processNotification_mailFails_registersNotificationFailed() throws Exception {
        given(identityClient.getPreferences(USER_ID)).willReturn(prefsEnabled());
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage((jakarta.mail.Session) null));
        willThrow(new MailSendException("SMTP error")).given(mailSender).send(any(MimeMessage.class));

        service.processNotification(buildRequest(USER_EMAIL));

        verify(auditClient).sendEvent(argThat(e -> "NOTIFICATION_FAILED".equals(e.getEventType())));
    }

    private UserPreferencesResponse prefsEnabled() {
        UserPreferencesResponse prefs = new UserPreferencesResponse();
        prefs.setOrderConfirm(true);
        prefs.setNotifEmail(true);
        return prefs;
    }

    private NotificationRequest buildRequest(String email) {
        NotificationRequest req = new NotificationRequest();
        req.setUserId(USER_ID);
        req.setEventType("ORDER_EXECUTED");
        Map<String, Object> data = new HashMap<>();
        data.put("userEmail", email);
        data.put("symbol", "AAPL");
        data.put("side", "buy");
        data.put("quantity", 1);
        data.put("executedPrice", new BigDecimal("150.00"));
        data.put("commission", new BigDecimal("3.00"));
        data.put("newBalance", new BigDecimal("49847.00"));
        req.setData(data);
        return req;
    }
}
