package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.client.AlpacaClockClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.MarketsServiceClient;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.request.PlaceOrderRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.QueuedOrderResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.InsufficientFundsException;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.InsufficientHoldingsException;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.OrderCancelNotAllowedException;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.OrderNotFoundException;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Account;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Holding;
import co.edu.unbosque.acciones_elbosque.trading_service.model.HoldingId;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Order;
import co.edu.unbosque.acciones_elbosque.trading_service.model.OrderStatus;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.AccountRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.HoldingRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.OrderRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.service.OrderService;
import co.edu.unbosque.acciones_elbosque.trading_service.service.QueuedOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class QueuedOrderServiceImpl implements QueuedOrderService {

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("50000.00");
    private static final String QUEUE_MESSAGE = "Tu orden quedó encolada y se ejecutará cuando el mercado abra.";

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final OrderRepository orderRepository;
    private final MarketsServiceClient marketsClient;
    private final AlpacaClockClient alpacaClockClient;
    private final OrderService orderService;
    private final BigDecimal commissionRate;

    public QueuedOrderServiceImpl(
            AccountRepository accountRepository,
            HoldingRepository holdingRepository,
            OrderRepository orderRepository,
            MarketsServiceClient marketsClient,
            AlpacaClockClient alpacaClockClient,
            OrderService orderService,
            @Value("${commission.percentage}") BigDecimal commissionRate) {
        this.accountRepository = accountRepository;
        this.holdingRepository = holdingRepository;
        this.orderRepository = orderRepository;
        this.marketsClient = marketsClient;
        this.alpacaClockClient = alpacaClockClient;
        this.orderService = orderService;
        this.commissionRate = commissionRate;
    }

    @Override
    @Transactional
    public QueuedOrderResponse enqueueOrder(PlaceOrderRequest request,
                                            UUID userId,
                                            String alpacaAccountId,
                                            String side,
                                            String token,
                                            String userEmail) {
        BigDecimal estimatedPrice = marketsClient.getStockPrice(request.getSymbol(), token).getPrice();

        if ("buy".equals(side)) {
            validateFundsForBuy(userId, estimatedPrice, request.getQuantity());
        } else {
            validateHoldingsForSell(userId, request.getSymbol(), request.getQuantity());
        }

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .symbol(request.getSymbol())
                .side(side)
                .orderType(request.getOrderType())
                .quantity(request.getQuantity())
                .estimatedPrice(estimatedPrice)
                .alpacaAccountId(alpacaAccountId)
                .userEmail(userEmail)
                .status(OrderStatus.PENDING_QUEUE)
                .createdAt(Instant.now())
                .build();
        orderRepository.save(order);

        return new QueuedOrderResponse(
                order.getId().toString(),
                order.getSymbol(),
                side,
                order.getQuantity(),
                estimatedPrice,
                "PENDING_QUEUE",
                order.getCreatedAt(),
                QUEUE_MESSAGE
        );
    }

    @Override
    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException();
        }
        if (order.getStatus() != OrderStatus.PENDING_QUEUE) {
            throw new OrderCancelNotAllowedException();
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    public void processQueue() {
        if (!alpacaClockClient.isMarketOpen()) {
            return;
        }

        List<Order> pending = orderRepository.findAllByStatusOrderByCreatedAtAsc(OrderStatus.PENDING_QUEUE);
        for (Order order : pending) {
            executeQueued(order);
        }
    }

    @SuppressWarnings("java:S2221")
    private void executeQueued(Order order) {
        try {
            if ("buy".equals(order.getSide())) {
                orderService.executeQueuedBuyOrder(order, order.getAlpacaAccountId());
            } else {
                orderService.executeQueuedSellOrder(order, order.getAlpacaAccountId());
            }
        } catch (InsufficientFundsException | InsufficientHoldingsException e) {
            log.warn("Queued order {} failed validation at execution: {}", order.getId(), e.getMessage());
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
        } catch (Exception e) {
            // Intentional broad catch: one failing order must not stop processing remaining queue
            log.error("Unexpected error processing queued order {}: {}", order.getId(), e.getMessage());
        }
    }

    private void validateFundsForBuy(UUID userId, BigDecimal estimatedPrice, int qty) {
        BigDecimal commission = estimatedPrice.multiply(BigDecimal.valueOf(qty))
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedTotal = estimatedPrice.multiply(BigDecimal.valueOf(qty)).add(commission);

        Account account = accountRepository.findById(userId)
                .orElseGet(() -> accountRepository.save(new Account(userId, INITIAL_BALANCE)));

        if (account.getBalance().compareTo(estimatedTotal) < 0) {
            throw new InsufficientFundsException();
        }
    }

    private void validateHoldingsForSell(UUID userId, String symbol, int qty) {
        HoldingId holdingId = new HoldingId(userId, symbol);
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(InsufficientHoldingsException::new);

        if (holding.getQuantity() < qty) {
            throw new InsufficientHoldingsException();
        }
    }
}
