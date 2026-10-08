package co.edu.unbosque.acciones_elbosque.trading_service.dto.response;

import java.util.List;

public record OrderHistoryResponse(
        List<OrderHistoryItem> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
