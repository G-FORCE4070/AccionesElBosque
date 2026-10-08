package co.edu.unbosque.acciones_elbosque.trading_service.client;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.MarketStatusDto;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.StockPriceDto;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.PriceServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MarketsServiceClient {

    private final RestClient restClient;
    private final String marketsUrl;

    public MarketsServiceClient(
            RestClient.Builder builder,
            @Value("${services.markets-url}") String marketsUrl) {
        this.restClient = builder.build();
        this.marketsUrl = marketsUrl;
    }

    public MarketStatusDto getMarketStatus(String symbol, String token) {
        try {
            return restClient.get()
                    .uri(marketsUrl + "/api/v1/markets/status/{symbol}", symbol)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(MarketStatusDto.class);
        } catch (RestClientException e) {
            throw new PriceServiceUnavailableException();
        }
    }

    public StockPriceDto getStockPrice(String symbol, String token) {
        try {
            return restClient.get()
                    .uri(marketsUrl + "/api/v1/markets/stocks/{symbol}", symbol)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(StockPriceDto.class);
        } catch (RestClientException e) {
            throw new PriceServiceUnavailableException();
        }
    }
}
