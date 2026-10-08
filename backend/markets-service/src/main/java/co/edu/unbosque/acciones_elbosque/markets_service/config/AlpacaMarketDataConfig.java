package co.edu.unbosque.acciones_elbosque.markets_service.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class AlpacaMarketDataConfig {

    @Value("${alpaca.market-data.base-url}")
    private String baseUrl;

    @Value("${alpaca.market-data.api-key}")
    private String apiKey;

    @Value("${alpaca.market-data.secret-key}")
    private String secretKey;

    @PostConstruct
    public void validateConfig() {
        if ("test-key".equals(apiKey)) {
            log.warn("ALPACA_MARKET_DATA_API_KEY is placeholder — prices will be null for all symbols");
        } else {
            log.info("Alpaca Market Data configured: baseUrl={}, key={}...",
                    baseUrl, apiKey.substring(0, Math.min(8, apiKey.length())));
        }
    }

    @Bean(name = "alpacaMarketDataClient")
    public RestClient alpacaMarketDataClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("APCA-API-KEY-ID", apiKey)
                .defaultHeader("APCA-API-SECRET-KEY", secretKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
