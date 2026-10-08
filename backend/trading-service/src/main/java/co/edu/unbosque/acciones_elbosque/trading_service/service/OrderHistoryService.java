package co.edu.unbosque.acciones_elbosque.trading_service.service;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.OrderHistoryResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface OrderHistoryService {

    OrderHistoryResponse getOrders(UUID userId, String symbol, String status,
                                   LocalDate startDate, LocalDate endDate,
                                   int page, int size);
}
