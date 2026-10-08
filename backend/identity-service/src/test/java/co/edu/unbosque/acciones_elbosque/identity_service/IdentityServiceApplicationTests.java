package co.edu.unbosque.acciones_elbosque.identity_service;

import co.edu.unbosque.acciones_elbosque.identity_service.service.AlpacaBrokerService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.StripeCheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@Testcontainers
class IdentityServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withInitScript("test-init.sql");

    @MockitoBean
    AlpacaBrokerService alpacaBrokerService;

    @MockitoBean
    StripeCheckoutService stripeCheckoutService;

    @MockitoBean
    JavaMailSender mailSender;

    @MockitoBean
    AuditService auditService;

    @Test
    void contextLoads() {
    }
}
