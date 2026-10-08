package co.edu.unbosque.acciones_elbosque.identity_service.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class AlpacaTokenService {

    @Value("${alpaca.broker.api-key}")
    private String apiKey;

    @Value("${alpaca.broker.secret-key}")
    private String secretKey;

    @Value("${alpaca.broker.auth-url}")
    private String authUrl;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    @PostConstruct
    public void warmUp() {
        if (!"test-key".equals(apiKey)) {
            try {
                refreshToken();
            } catch (Exception e) {
                log.error("Alpaca token warm-up failed: {} — will retry on first request", e.getMessage());
            }
        }
    }

    public String getToken() {
        if (cachedToken == null || Instant.now().isAfter(tokenExpiry.minusSeconds(60))) {
            refreshToken();
        }
        return cachedToken;
    }

    private synchronized void refreshToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
            return;
        }
        if ("test-key".equals(apiKey)) {
            log.warn("Alpaca credentials are placeholders — token refresh skipped");
            cachedToken = "placeholder-token";
            tokenExpiry = Instant.now().plusSeconds(900);
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", apiKey);
        form.add("client_secret", secretKey);

        RestClient authClient = RestClient.builder().build();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = authClient.post()
                .uri(authUrl + "/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        cachedToken = (String) response.get("access_token");
        Number expiresIn = (Number) response.getOrDefault("expires_in", 900);
        tokenExpiry = Instant.now().plusSeconds(expiresIn.longValue());
        log.info("Alpaca Bearer token refreshed, expires in {}s", expiresIn);
    }
}
