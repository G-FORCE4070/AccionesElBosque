package co.edu.unbosque.acciones_elbosque.trading_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record QueuedOrderResponse(
        String orderId,
        String symbol,
        String side,
        int quantity,
        BigDecimal estimatedPrice,
        String status,
        Instant queuedAt,
        String message
) {}
