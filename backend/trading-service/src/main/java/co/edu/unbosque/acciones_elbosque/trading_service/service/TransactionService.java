package co.edu.unbosque.acciones_elbosque.trading_service.service;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.TransactionHistoryResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface TransactionService {

    TransactionHistoryResponse getTransactions(UUID userId, String type, String symbol,
                                               LocalDate startDate, LocalDate endDate,
                                               int page, int size);
}
