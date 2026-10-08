package co.edu.unbosque.acciones_elbosque.notification_service;

import co.edu.unbosque.acciones_elbosque.notification_service.client.AuditClient;
import co.edu.unbosque.acciones_elbosque.notification_service.client.IdentityClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class NotificationServiceApplicationTests {

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean IdentityClient identityClient;
    @MockitoBean AuditClient auditClient;

    @Test
    void contextLoads() {
    }
}
