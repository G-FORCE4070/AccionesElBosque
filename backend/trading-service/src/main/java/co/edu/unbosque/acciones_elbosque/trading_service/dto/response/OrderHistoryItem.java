package co.edu.unbosque.acciones_elbosque.trading_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderHistoryItem(
        String orderId,
        String symbol,
        String side,
        String orderType,
        int quantity,
        BigDecimal executedPrice,
        BigDecimal commission,
        String status,
        Instant createdAt,
        Instant executedAt
) {}
