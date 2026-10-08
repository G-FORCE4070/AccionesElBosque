package co.edu.unbosque.acciones_elbosque.trading_service.controller;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.request.PlaceOrderRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.OrderResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.QueuedOrderResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.MarketClosedException;
import co.edu.unbosque.acciones_elbosque.trading_service.service.OrderService;
import co.edu.unbosque.acciones_elbosque.trading_service.service.QueuedOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final QueuedOrderService queuedOrderService;

    public OrderController(OrderService orderService, QueuedOrderService queuedOrderService) {
        this.orderService = orderService;
        this.queuedOrderService = queuedOrderService;
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buy(
            @Valid @RequestBody PlaceOrderRequest request,
            Authentication auth,
            HttpServletRequest httpRequest,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = UUID.fromString(auth.getName());
        String alpacaAccountId = (String) httpRequest.getAttribute("alpacaAccountId");
        String userEmail = (String) httpRequest.getAttribute("email");
        String token = authHeader.substring(7);
        try {
            OrderResponse response = orderService.placeBuyOrder(request, userId, alpacaAccountId, token, userEmail);
            return ResponseEntity.status(201).body(response);
        } catch (MarketClosedException e) {
            if (request.isQueue()) {
                QueuedOrderResponse queued = queuedOrderService.enqueueOrder(request, userId, alpacaAccountId, "buy", token, userEmail);
                return ResponseEntity.accepted().body(queued);
            }
            throw e;
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID orderId,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        queuedOrderService.cancelOrder(orderId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sell")
    public ResponseEntity<?> sell(
            @Valid @RequestBody PlaceOrderRequest request,
            Authentication auth,
            HttpServletRequest httpRequest,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = UUID.fromString(auth.getName());
        String alpacaAccountId = (String) httpRequest.getAttribute("alpacaAccountId");
        String userEmail = (String) httpRequest.getAttribute("email");
        String token = authHeader.substring(7);
        try {
            OrderResponse response = orderService.placeSellOrder(request, userId, alpacaAccountId, token, userEmail);
            return ResponseEntity.status(201).body(response);
        } catch (MarketClosedException e) {
            if (request.isQueue()) {
                QueuedOrderResponse queued = queuedOrderService.enqueueOrder(request, userId, alpacaAccountId, "sell", token, userEmail);
                return ResponseEntity.accepted().body(queued);
            }
            throw e;
        }
    }
}
