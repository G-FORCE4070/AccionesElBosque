package co.edu.unbosque.acciones_elbosque.trading_service.service;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.request.PlaceOrderRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.OrderResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Order;

import java.util.UUID;

public interface OrderService {

    OrderResponse placeBuyOrder(PlaceOrderRequest request, UUID userId, String alpacaAccountId, String token, String userEmail);

    OrderResponse placeSellOrder(PlaceOrderRequest request, UUID userId, String alpacaAccountId, String token, String userEmail);

    OrderResponse executeQueuedBuyOrder(Order pendingOrder, String alpacaAccountId);

    OrderResponse executeQueuedSellOrder(Order pendingOrder, String alpacaAccountId);
}
