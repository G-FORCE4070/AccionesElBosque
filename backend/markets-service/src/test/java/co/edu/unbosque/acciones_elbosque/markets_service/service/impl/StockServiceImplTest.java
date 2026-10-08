package co.edu.unbosque.acciones_elbosque.markets_service.service.impl;

import co.edu.unbosque.acciones_elbosque.markets_service.dto.response.StockResponse;
import co.edu.unbosque.acciones_elbosque.markets_service.model.AlpacaPriceData;
import co.edu.unbosque.acciones_elbosque.markets_service.model.Stock;
import co.edu.unbosque.acciones_elbosque.markets_service.repository.StockRepository;
import co.edu.unbosque.acciones_elbosque.markets_service.service.AlpacaClockService;
import co.edu.unbosque.acciones_elbosque.markets_service.service.AlpacaMarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock StockRepository stockRepository;
    @Mock AlpacaMarketDataService alpacaMarketDataService;
    @Mock AlpacaClockService alpacaClockService;

    @InjectMocks StockServiceImpl stockService;

    private Stock aapl;
    private Stock msft;
    private Stock jnj;

    @BeforeEach
    void setUp() {
        aapl = buildStock("AAPL", "Apple Inc.", "NASDAQ", "tech");
        msft = buildStock("MSFT", "Microsoft Corp.", "NASDAQ", "tech");
        jnj  = buildStock("JNJ",  "Johnson & Johnson", "NYSE", "health");

        given(alpacaMarketDataService.getPrice(anyString()))
                .willReturn(new AlpacaPriceData(150.0, 1.5));
    }

    @Test
    void getStocks_noFilters_returnsAll() {
        given(stockRepository.findByActiveTrue()).willReturn(List.of(aapl, msft, jnj));

        List<StockResponse> result = stockService.getStocks(null, null);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(StockResponse::symbol)
                .containsExactlyInAnyOrder("AAPL", "MSFT", "JNJ");
    }

    @Test
    void getStocks_marketFilter_returnsOnlyNasdaq() {
        given(stockRepository.findByActiveTrueAndMarketCodeIn(List.of("NASDAQ")))
                .willReturn(List.of(aapl, msft));

        List<StockResponse> result = stockService.getStocks("nasdaq", null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(StockResponse::market)
                .containsOnly("NASDAQ");
    }

    @Test
    void getStocks_sectorFilter_returnsOnlyHealth() {
        given(stockRepository.findByActiveTrueAndSectorIn(List.of("health")))
                .willReturn(List.of(jnj));

        List<StockResponse> result = stockService.getStocks(null, "health");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).symbol()).isEqualTo("JNJ");
    }

    @Test
    void getStocks_combinedFilter_returnsSingleMatch() {
        given(stockRepository.findByActiveTrueAndMarketCodeInAndSectorIn(
                List.of("NYSE"), List.of("health")))
                .willReturn(List.of(jnj));

        List<StockResponse> result = stockService.getStocks("nyse", "health");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).symbol()).isEqualTo("JNJ");
    }

    private Stock buildStock(String symbol, String name, String market, String sector) {
        try {
            Stock s = new Stock();
            setField(s, "symbol", symbol);
            setField(s, "companyName", name);
            setField(s, "marketCode", market);
            setField(s, "sector", sector);
            setField(s, "active", true);
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object obj, String field, Object value) throws Exception {
        var f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, value);
    }
}
