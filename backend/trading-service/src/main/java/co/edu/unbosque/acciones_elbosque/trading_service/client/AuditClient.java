package co.edu.unbosque.acciones_elbosque.trading_service.client;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.AuditEventRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendEvent(AuditEventRequest request) {
        try {
            restClient.post()
                    .uri(auditUrl + "/api/v1/audit/events")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ignored) {
            // audit is non-critical; log silently on failure
        }
    }
}
