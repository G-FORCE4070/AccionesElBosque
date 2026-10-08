package co.edu.unbosque.acciones_elbosque.trading_service.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TransactionHistoryResponse(
        List<TransactionItem> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        BigDecimal totalCommission
) {}
