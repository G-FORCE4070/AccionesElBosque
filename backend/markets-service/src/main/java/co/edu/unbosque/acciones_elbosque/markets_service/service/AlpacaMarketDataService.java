package co.edu.unbosque.acciones_elbosque.markets_service.service;

import co.edu.unbosque.acciones_elbosque.markets_service.model.AlpacaPriceData;

public interface AlpacaMarketDataService {

    AlpacaPriceData getPrice(String symbol);
}
