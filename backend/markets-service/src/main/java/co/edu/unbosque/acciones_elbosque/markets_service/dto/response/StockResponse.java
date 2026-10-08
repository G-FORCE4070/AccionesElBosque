package co.edu.unbosque.acciones_elbosque.markets_service.dto.response;

public record StockResponse(
        String symbol,
        String companyName,
        String market,
        String sector,
        Double price,
        Double change24h
) {}
