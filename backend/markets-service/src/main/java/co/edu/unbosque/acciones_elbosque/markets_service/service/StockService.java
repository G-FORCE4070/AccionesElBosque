package co.edu.unbosque.acciones_elbosque.markets_service.service;

import co.edu.unbosque.acciones_elbosque.markets_service.dto.response.MarketStatusResponse;
import co.edu.unbosque.acciones_elbosque.markets_service.dto.response.StockResponse;

import java.util.List;

public interface StockService {

    List<StockResponse> getStocks(String markets, String sectors);

    StockResponse getStock(String symbol);

    MarketStatusResponse getMarketStatus(String symbol);
}
