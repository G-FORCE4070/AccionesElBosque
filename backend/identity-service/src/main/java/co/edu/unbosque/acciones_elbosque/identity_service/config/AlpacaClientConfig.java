package co.edu.unbosque.acciones_elbosque.identity_service.config;

import co.edu.unbosque.acciones_elbosque.identity_service.service.AlpacaTokenService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class AlpacaClientConfig {

    @Value("${alpaca.broker.base-url}")
    private String baseUrl;

    @Value("${alpaca.broker.api-key}")
    private String apiKey;

    @PostConstruct
    public void logConfig() {
        if ("test-key".equals(apiKey)) {
            log.warn("ALPACA_BROKER_API_KEY is placeholder — all Alpaca calls will fail");
        } else {
            log.info("Alpaca Broker configured: baseUrl={}, key={}...", baseUrl, apiKey.substring(0, Math.min(8, apiKey.length())));
        }
    }

    @Bean(name = "alpacaRestClient")
    public RestClient alpacaRestClient(AlpacaTokenService tokenService) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenService.getToken());
                    return execution.execute(request, body);
                })
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
