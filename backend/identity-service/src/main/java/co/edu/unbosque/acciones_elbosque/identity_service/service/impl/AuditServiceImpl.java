package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AuditServiceImpl implements AuditService {

    @Value("${app.audit.url}")
    private String auditServiceUrl;

    @Override
    @Async("asyncExecutor")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendEvent(String eventType, String userId, Map<String, Object> details) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("timestamp", Instant.now().toString());
            event.put("service", "identity-service");
            event.put("eventType", eventType);
            event.put("userId", userId);
            event.put("correlationId", UUID.randomUUID().toString());
            event.put("details", details);

            RestClient.create().post()
                    .uri(auditServiceUrl + "/api/v1/internal/events")
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("Failed to send audit event {} for user {}: {}", eventType, userId, ex.getMessage());
        }
    }
}
