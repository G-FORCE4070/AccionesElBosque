package co.edu.unbosque.acciones_elbosque.markets_service.service.impl;

import co.edu.unbosque.acciones_elbosque.markets_service.dto.response.MarketStatusResponse;
import co.edu.unbosque.acciones_elbosque.markets_service.dto.response.StockResponse;
import co.edu.unbosque.acciones_elbosque.markets_service.model.AlpacaPriceData;
import co.edu.unbosque.acciones_elbosque.markets_service.model.Stock;
import co.edu.unbosque.acciones_elbosque.markets_service.repository.StockRepository;
import co.edu.unbosque.acciones_elbosque.markets_service.service.AlpacaClockService;
import co.edu.unbosque.acciones_elbosque.markets_service.service.AlpacaMarketDataService;
import co.edu.unbosque.acciones_elbosque.markets_service.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final AlpacaMarketDataService alpacaMarketDataService;
    private final AlpacaClockService alpacaClockService;

    @Override
    public List<StockResponse> getStocks(String marketsParam, String sectorsParam) {
        boolean hasMarkets = marketsParam != null && !marketsParam.isBlank();
        boolean hasSectors = sectorsParam != null && !sectorsParam.isBlank();

        List<String> markets = hasMarkets
                ? Arrays.stream(marketsParam.split(",")).map(String::trim).map(String::toUpperCase).toList()
                : null;
        List<String> sectors = hasSectors
                ? Arrays.stream(sectorsParam.split(",")).map(String::trim).map(String::toLowerCase).toList()
                : null;

        List<Stock> stocks;
        if (hasMarkets && hasSectors) {
            stocks = stockRepository.findByActiveTrueAndMarketCodeInAndSectorIn(markets, sectors);
        } else if (hasMarkets) {
            stocks = stockRepository.findByActiveTrueAndMarketCodeIn(markets);
        } else if (hasSectors) {
            stocks = stockRepository.findByActiveTrueAndSectorIn(sectors);
        } else {
            stocks = stockRepository.findByActiveTrue();
        }

        return stocks.stream().map(this::toResponse).toList();
    }

    @Override
    public StockResponse getStock(String symbol) {
        Stock stock = stockRepository.findById(symbol.toUpperCase())
                .filter(Stock::isActive)
                .orElseThrow(() -> new NoSuchElementException("Símbolo no encontrado: " + symbol));
        return toResponse(stock);
    }

    @Override
    public MarketStatusResponse getMarketStatus(String symbol) {
        Stock stock = stockRepository.findById(symbol.toUpperCase())
                .filter(Stock::isActive)
                .orElseThrow(() -> new NoSuchElementException("Símbolo no encontrado: " + symbol));

        boolean open = alpacaClockService.isMarketOpen();
        return new MarketStatusResponse(symbol.toUpperCase(), stock.getMarketCode(), open, null, null);
    }

    private StockResponse toResponse(Stock stock) {
        AlpacaPriceData data = alpacaMarketDataService.getPrice(stock.getSymbol());
        return new StockResponse(
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getMarketCode(),
                stock.getSector(),
                data != null ? data.price() : null,
                data != null ? data.change24h() : null
        );
    }
}
