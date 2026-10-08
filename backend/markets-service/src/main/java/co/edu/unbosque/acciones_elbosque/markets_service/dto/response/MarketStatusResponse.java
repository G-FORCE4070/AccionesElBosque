package co.edu.unbosque.acciones_elbosque.markets_service.dto.response;

public record MarketStatusResponse(
        String symbol,
        String market,
        boolean open,
        String opensAt,
        String closesAt
) {}
