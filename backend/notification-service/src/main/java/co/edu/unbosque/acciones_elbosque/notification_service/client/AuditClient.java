package co.edu.unbosque.acciones_elbosque.notification_service.client;

import co.edu.unbosque.acciones_elbosque.notification_service.dto.AuditEventRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class AuditClient {

    private final RestClient restClient;
    private final String auditUrl;

    public AuditClient(
            RestClient.Builder builder,
            @Value("${services.audit-url}") String auditUrl) {
        this.restClient = builder.build();
        this.auditUrl = auditUrl;
    }

    @Async
    public void sendEvent(AuditEventRequest request) {
        try {
            restClient.post()
                    .uri(auditUrl + "/api/v1/internal/events")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Audit event could not be sent: {}", e.getMessage());
        }
    }
}
