package co.edu.unbosque.acciones_elbosque.notification_service.client;

import co.edu.unbosque.acciones_elbosque.notification_service.dto.UserPreferencesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class IdentityClient {

    private final RestClient restClient;
    private final String identityUrl;

    public IdentityClient(
            RestClient.Builder builder,
            @Value("${services.identity-url}") String identityUrl) {
        this.restClient = builder
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().add("Connection", "keep-alive");
                    return execution.execute(request, body);
                })
                .build();
        this.identityUrl = identityUrl;
    }

    public UserPreferencesResponse getPreferences(String userId) {
        return restClient.get()
                .uri(identityUrl + "/api/v1/internal/users/" + userId + "/preferences")
                .retrieve()
                .body(UserPreferencesResponse.class);
    }
}
