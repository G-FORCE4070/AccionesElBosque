package co.edu.unbosque.acciones_elbosque.trading_service.client;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.NotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationClient {

    private final RestClient restClient;
    private final String notificationUrl;

    public NotificationClient(
            RestClient.Builder builder,
            @Value("${services.notification-url}") String notificationUrl) {
        this.restClient = builder.build();
        this.notificationUrl = notificationUrl;
    }

    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void notifyOrderExecuted(NotificationRequest request) {
        try {
            restClient.post()
                    .uri(notificationUrl + "/api/v1/internal/notifications")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ignored) {
            // notifications are non-critical; log silently on failure
        }
    }
}
