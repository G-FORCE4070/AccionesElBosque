package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.client.AlpacaBrokerClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.AuditClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.MarketsServiceClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.NotificationClient;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.AlpacaOrderRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.AlpacaOrderResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.AuditEventRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.NotificationRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.request.PlaceOrderRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.OrderResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.InsufficientFundsException;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.InsufficientHoldingsException;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.MarketClosedException;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Account;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Holding;
import co.edu.unbosque.acciones_elbosque.trading_service.model.HoldingId;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Order;
import co.edu.unbosque.acciones_elbosque.trading_service.model.OrderStatus;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.AccountRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.HoldingRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.OrderRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final int POLL_ATTEMPTS = 5;
    private static final long POLL_DELAY_MS = 1000L;
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("50000.00");

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final OrderRepository orderRepository;
    private final MarketsServiceClient marketsClient;
    private final AlpacaBrokerClient alpacaClient;
    private final AuditClient auditClient;
    private final NotificationClient notificationClient;
    private final BigDecimal commissionRate;

    public OrderServiceImpl(
            AccountRepository accountRepository,
            HoldingRepository holdingRepository,
            OrderRepository orderRepository,
            MarketsServiceClient marketsClient,
            AlpacaBrokerClient alpacaClient,
            AuditClient auditClient,
            NotificationClient notificationClient,
            @Value("${commission.percentage}") BigDecimal commissionRate) {
        this.accountRepository = accountRepository;
        this.holdingRepository = holdingRepository;
        this.orderRepository = orderRepository;
        this.marketsClient = marketsClient;
        this.alpacaClient = alpacaClient;
        this.auditClient = auditClient;
        this.notificationClient = notificationClient;
        this.commissionRate = commissionRate;
    }

    @Override
    @Transactional
    public OrderResponse placeBuyOrder(PlaceOrderRequest request, UUID userId, String alpacaAccountId, String token, String userEmail) {
        validateMarketOpen(request.getSymbol(), token);

        BigDecimal estimatedPrice = fetchEstimatedPrice(request.getSymbol(), token);
        BigDecimal commission = calculateCommission(estimatedPrice, request.getQuantity());
        BigDecimal estimatedTotal = estimatedPrice.multiply(BigDecimal.valueOf(request.getQuantity())).add(commission);

        Account account = getOrCreateAccount(userId);
        if (account.getBalance().compareTo(estimatedTotal) < 0) {
            throw new InsufficientFundsException();
        }

        Order order = saveOrderSent(userId, request, "buy", estimatedPrice, userEmail);
        AlpacaOrderResponse filled = executeThroughAlpaca(alpacaAccountId, request.getSymbol(), request.getQuantity(), "buy", order);

        BigDecimal executedPrice = resolveExecutedPrice(filled, estimatedPrice);
        BigDecimal actualCommission = calculateCommission(executedPrice, request.getQuantity());

        applyBuyExecution(userId, request.getSymbol(), request.getQuantity(), executedPrice, actualCommission);
        order = finalizeOrder(order, executedPrice, actualCommission, filled != null ? filled.getId() : null);

        account = accountRepository.findById(userId).orElseThrow();
        dispatchAsyncEvents(userId, order, account.getBalance(), "buy");

        return buildOrderResponse(order, account.getBalance(), actualCommission);
    }

    @Override
    @Transactional
    public OrderResponse placeSellOrder(PlaceOrderRequest request, UUID userId, String alpacaAccountId, String token, String userEmail) {
        validateMarketOpen(request.getSymbol(), token);

        BigDecimal estimatedPrice = fetchEstimatedPrice(request.getSymbol(), token);

        Account account = getOrCreateAccount(userId);
        HoldingId holdingId = new HoldingId(userId, request.getSymbol());
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(InsufficientHoldingsException::new);

        if (holding.getQuantity() < request.getQuantity()) {
            throw new InsufficientHoldingsException();
        }

        Order order = saveOrderSent(userId, request, "sell", estimatedPrice, userEmail);
        AlpacaOrderResponse filled = executeThroughAlpaca(alpacaAccountId, request.getSymbol(), request.getQuantity(), "sell", order);

        BigDecimal executedPrice = resolveExecutedPrice(filled, estimatedPrice);
        BigDecimal actualCommission = calculateCommission(executedPrice, request.getQuantity());

        applySellExecution(userId, request.getSymbol(), request.getQuantity(), executedPrice, actualCommission, holding);
        order = finalizeOrder(order, executedPrice, actualCommission, filled != null ? filled.getId() : null);

        account = accountRepository.findById(userId).orElseThrow();
        dispatchAsyncEvents(userId, order, account.getBalance(), "sell");

        return buildOrderResponse(order, account.getBalance(), actualCommission);
    }

    private void validateMarketOpen(String symbol, String token) {
        var status = marketsClient.getMarketStatus(symbol, token);
        if (status == null || !status.isOpen()) {
            throw new MarketClosedException();
        }
    }

    private BigDecimal fetchEstimatedPrice(String symbol, String token) {
        var stock = marketsClient.getStockPrice(symbol, token);
        return stock.getPrice();
    }

    private BigDecimal calculateCommission(BigDecimal price, int qty) {
        return price.multiply(BigDecimal.valueOf(qty))
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Account getOrCreateAccount(UUID userId) {
        return accountRepository.findById(userId)
                .orElseGet(() -> accountRepository.save(new Account(userId, INITIAL_BALANCE)));
    }

    private Order saveOrderSent(UUID userId, PlaceOrderRequest request, String side, BigDecimal estimatedPrice, String userEmail) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .symbol(request.getSymbol())
                .side(side)
                .orderType(request.getOrderType())
                .quantity(request.getQuantity())
                .estimatedPrice(estimatedPrice)
                .userEmail(userEmail)
                .status(OrderStatus.SENT)
                .createdAt(Instant.now())
                .build();
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrderResponse executeQueuedBuyOrder(Order pending, String alpacaAccountId) {
        Account account = getOrCreateAccount(pending.getUserId());
        BigDecimal commission = calculateCommission(pending.getEstimatedPrice(), pending.getQuantity());
        BigDecimal estimatedTotal = pending.getEstimatedPrice()
                .multiply(BigDecimal.valueOf(pending.getQuantity()))
                .add(commission);

        if (account.getBalance().compareTo(estimatedTotal) < 0) {
            throw new InsufficientFundsException();
        }

        pending.setStatus(OrderStatus.SENT);
        orderRepository.save(pending);

        AlpacaOrderResponse filled = executeThroughAlpaca(alpacaAccountId,
                pending.getSymbol(), pending.getQuantity(), "buy", pending);

        BigDecimal executedPrice = resolveExecutedPrice(filled, pending.getEstimatedPrice());
        BigDecimal actualCommission = calculateCommission(executedPrice, pending.getQuantity());

        applyBuyExecution(pending.getUserId(), pending.getSymbol(), pending.getQuantity(), executedPrice, actualCommission);
        Order order = finalizeOrder(pending, executedPrice, actualCommission, filled != null ? filled.getId() : null);

        account = accountRepository.findById(pending.getUserId()).orElseThrow();
        dispatchAsyncEvents(pending.getUserId(), order, account.getBalance(), "buy");

        return buildOrderResponse(order, account.getBalance(), actualCommission);
    }

    @Override
    @Transactional
    public OrderResponse executeQueuedSellOrder(Order pending, String alpacaAccountId) {
        HoldingId holdingId = new HoldingId(pending.getUserId(), pending.getSymbol());
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(InsufficientHoldingsException::new);

        if (holding.getQuantity() < pending.getQuantity()) {
            throw new InsufficientHoldingsException();
        }

        pending.setStatus(OrderStatus.SENT);
        orderRepository.save(pending);

        AlpacaOrderResponse filled = executeThroughAlpaca(alpacaAccountId,
                pending.getSymbol(), pending.getQuantity(), "sell", pending);

        BigDecimal executedPrice = resolveExecutedPrice(filled, pending.getEstimatedPrice());
        BigDecimal actualCommission = calculateCommission(executedPrice, pending.getQuantity());

        applySellExecution(pending.getUserId(), pending.getSymbol(), pending.getQuantity(),
                executedPrice, actualCommission, holding);
        Order order = finalizeOrder(pending, executedPrice, actualCommission, filled != null ? filled.getId() : null);

        Account account = accountRepository.findById(pending.getUserId()).orElseThrow();
        dispatchAsyncEvents(pending.getUserId(), order, account.getBalance(), "sell");

        return buildOrderResponse(order, account.getBalance(), actualCommission);
    }

    private AlpacaOrderResponse executeThroughAlpaca(
            String alpacaAccountId, String symbol, int qty, String side, Order order) {
        AlpacaOrderRequest alpacaRequest = AlpacaOrderRequest.builder()
                .symbol(symbol)
                .qty(String.valueOf(qty))
                .side(side)
                .type("market")
                .timeInForce("day")
                .build();
        try {
            AlpacaOrderResponse submitted = alpacaClient.placeOrder(alpacaAccountId, alpacaRequest);
            order.setAlpacaOrderId(submitted.getId());
            return pollUntilFilled(alpacaAccountId, submitted.getId());
        } catch (RestClientException e) {
            log.warn("Alpaca order placement failed for account {}: {} — using estimated price as executed price",
                    alpacaAccountId, e.getMessage());
            return null;
        }
    }

    private AlpacaOrderResponse pollUntilFilled(String accountId, String alpacaOrderId) {
        AlpacaOrderResponse response = null;
        for (int i = 0; i < POLL_ATTEMPTS; i++) {
            response = alpacaClient.getOrderStatus(accountId, alpacaOrderId);
            if ("filled".equals(response.getStatus())) {
                break;
            }
            try {
                Thread.sleep(POLL_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return response;
    }

    private BigDecimal resolveExecutedPrice(AlpacaOrderResponse filled, BigDecimal estimatedPrice) {
        return (filled != null && filled.getFilledAvgPrice() != null)
                ? filled.getFilledAvgPrice()
                : estimatedPrice;
    }

    private void applyBuyExecution(UUID userId, String symbol, int qty, BigDecimal executedPrice, BigDecimal commission) {
        BigDecimal totalCost = executedPrice.multiply(BigDecimal.valueOf(qty)).add(commission);

        Account account = getOrCreateAccount(userId);
        account.setBalance(account.getBalance().subtract(totalCost));
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        HoldingId holdingId = new HoldingId(userId, symbol);
        holdingRepository.findById(holdingId).ifPresentOrElse(
                h -> updateHoldingAvgCost(h, qty, executedPrice),
                () -> holdingRepository.save(new Holding(holdingId, qty, executedPrice))
        );
    }

    private void updateHoldingAvgCost(Holding holding, int newQty, BigDecimal newPrice) {
        int oldQty = holding.getQuantity();
        BigDecimal oldAvgCost = holding.getAvgCost();
        BigDecimal newAvgCost = oldAvgCost.multiply(BigDecimal.valueOf(oldQty))
                .add(newPrice.multiply(BigDecimal.valueOf(newQty)))
                .divide(BigDecimal.valueOf((long) oldQty + newQty), 2, RoundingMode.HALF_UP);
        holding.setQuantity(oldQty + newQty);
        holding.setAvgCost(newAvgCost);
        holding.setUpdatedAt(Instant.now());
        holdingRepository.save(holding);
    }

    private void applySellExecution(UUID userId, String symbol, int qty, BigDecimal executedPrice,
                                    BigDecimal commission, Holding holding) {
        BigDecimal proceeds = executedPrice.multiply(BigDecimal.valueOf(qty)).subtract(commission);

        Account account = getOrCreateAccount(userId);
        account.setBalance(account.getBalance().add(proceeds));
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        int remainingQty = holding.getQuantity() - qty;
        if (remainingQty == 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(remainingQty);
            holding.setUpdatedAt(Instant.now());
            holdingRepository.save(holding);
        }
    }

    private Order finalizeOrder(Order order, BigDecimal executedPrice, BigDecimal commission, String alpacaOrderId) {
        order.setExecutedPrice(executedPrice);
        order.setCommission(commission);
        order.setStatus(OrderStatus.EXECUTED);
        order.setAlpacaOrderId(alpacaOrderId);
        order.setExecutedAt(Instant.now());
        return orderRepository.save(order);
    }

    private void dispatchAsyncEvents(UUID userId, Order order, BigDecimal newBalance, String side) {
        auditClient.sendEvent(AuditEventRequest.builder()
                .timestamp(Instant.now())
                .service("trading-service")
                .eventType("ORDER_EXECUTED")
                .userId(userId.toString())
                .correlationId(order.getId().toString())
                .details(Map.of(
                        "symbol", order.getSymbol(),
                        "side", side,
                        "quantity", order.getQuantity(),
                        "executedPrice", order.getExecutedPrice(),
                        "commission", order.getCommission()
                ))
                .build());

        Map<String, Object> data = new HashMap<>();
        data.put("symbol", order.getSymbol());
        data.put("side", side);
        data.put("quantity", order.getQuantity());
        data.put("executedPrice", order.getExecutedPrice());
        data.put("commission", order.getCommission());
        data.put("newBalance", newBalance);
        data.put("userEmail", order.getUserEmail());

        notificationClient.notifyOrderExecuted(NotificationRequest.builder()
                .userId(userId.toString())
                .eventType("ORDER_EXECUTED")
                .data(data)
                .build());
    }

    private OrderResponse buildOrderResponse(Order order, BigDecimal newBalance, BigDecimal commission) {
        BigDecimal total = order.getExecutedPrice()
                .multiply(BigDecimal.valueOf(order.getQuantity()));
        BigDecimal totalAmount = "buy".equals(order.getSide())
                ? total.add(commission)
                : total.subtract(commission);

        return OrderResponse.builder()
                .orderId(order.getId().toString())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .orderType(order.getOrderType())
                .quantity(order.getQuantity())
                .estimatedPrice(order.getEstimatedPrice())
                .executedPrice(order.getExecutedPrice())
                .commission(commission)
                .totalAmount(totalAmount)
                .newBalance(newBalance)
                .status(order.getStatus().name())
                .executedAt(order.getExecutedAt())
                .build();
    }
}
