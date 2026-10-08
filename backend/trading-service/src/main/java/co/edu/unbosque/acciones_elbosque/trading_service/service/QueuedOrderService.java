package co.edu.unbosque.acciones_elbosque.trading_service.service;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.request.PlaceOrderRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.QueuedOrderResponse;

import java.util.UUID;

public interface QueuedOrderService {

    QueuedOrderResponse enqueueOrder(PlaceOrderRequest request,
                                     UUID userId,
                                     String alpacaAccountId,
                                     String side,
                                     String token,
                                     String userEmail);

    void cancelOrder(UUID orderId, UUID userId);

    void processQueue();
}
