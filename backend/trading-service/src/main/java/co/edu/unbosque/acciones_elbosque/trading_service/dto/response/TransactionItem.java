package co.edu.unbosque.acciones_elbosque.trading_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionItem(
        String transactionId,
        Instant date,
        String type,
        String symbol,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal commission,
        BigDecimal total
) {}
