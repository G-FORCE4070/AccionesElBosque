package co.edu.unbosque.acciones_elbosque.markets_service.service.impl;

import co.edu.unbosque.acciones_elbosque.markets_service.dto.AlpacaSnapshotDto;
import co.edu.unbosque.acciones_elbosque.markets_service.model.AlpacaPriceData;
import co.edu.unbosque.acciones_elbosque.markets_service.service.AlpacaMarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlpacaMarketDataServiceImpl implements AlpacaMarketDataService {

    @Qualifier("alpacaMarketDataClient")
    private final RestClient alpacaMarketDataClient;

    @Value("${alpaca.market-data.api-key}")
    private String apiKey;

    @Override
    @Cacheable(value = "stock", key = "#symbol", unless = "#result == null")
    public AlpacaPriceData getPrice(String symbol) {
        if ("test-key".equals(apiKey)) {
            log.debug("Alpaca Market Data not configured — returning null price for {}", symbol);
            return null;
        }
        try {
            AlpacaSnapshotDto snapshot = alpacaMarketDataClient.get()
                    .uri("/stocks/{symbol}/snapshot?feed=iex", symbol)
                    .retrieve()
                    .body(AlpacaSnapshotDto.class);

            if (snapshot == null
                    || snapshot.getLatestTrade() == null
                    || snapshot.getPrevDailyBar() == null) {
                log.warn("Incomplete snapshot for {}", symbol);
                return null;
            }

            double price = snapshot.getLatestTrade().getP();
            double prevClose = snapshot.getPrevDailyBar().getC();
            double change24h = prevClose != 0 ? ((price - prevClose) / prevClose) * 100 : 0.0;

            return new AlpacaPriceData(price, change24h);
        } catch (RestClientException ex) {
            log.warn("Failed to fetch price for {}: {}", symbol, ex.getMessage());
            return null;
        }
    }
}
