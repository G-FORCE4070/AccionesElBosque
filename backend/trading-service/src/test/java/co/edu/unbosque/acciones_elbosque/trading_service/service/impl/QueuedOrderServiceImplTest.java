package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.client.AlpacaClockClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.MarketsServiceClient;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.StockPriceDto;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.request.PlaceOrderRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.QueuedOrderResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.InsufficientFundsException;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.InsufficientHoldingsException;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Account;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Holding;
import co.edu.unbosque.acciones_elbosque.trading_service.model.HoldingId;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Order;
import co.edu.unbosque.acciones_elbosque.trading_service.model.OrderStatus;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.AccountRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.HoldingRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.OrderRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueuedOrderServiceImplTest {

    @Mock AccountRepository accountRepository;
    @Mock HoldingRepository holdingRepository;
    @Mock OrderRepository orderRepository;
    @Mock MarketsServiceClient marketsClient;
    @Mock AlpacaClockClient alpacaClockClient;
    @Mock OrderService orderService;

    QueuedOrderServiceImpl service;

    static final UUID USER_ID = UUID.randomUUID();
    static final String ACCOUNT_ID = "alpaca-acc-1";
    static final String TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        service = new QueuedOrderServiceImpl(
                accountRepository, holdingRepository, orderRepository,
                marketsClient, alpacaClockClient, orderService,
                new BigDecimal("0.02")
        );
    }

    @Test
    void enqueue_buy_success() {
        PlaceOrderRequest request = buildRequest("AAPL", 2);
        given(marketsClient.getStockPrice("AAPL", TOKEN)).willReturn(price("100.00"));
        given(accountRepository.findById(USER_ID)).willReturn(Optional.of(new Account(USER_ID, new BigDecimal("50000.00"))));
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        QueuedOrderResponse result = service.enqueueOrder(request, USER_ID, ACCOUNT_ID, "buy", TOKEN, null);

        assertThat(result.status()).isEqualTo("PENDING_QUEUE");
        assertThat(result.symbol()).isEqualTo("AAPL");
        assertThat(result.side()).isEqualTo("buy");
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PENDING_QUEUE
                && ACCOUNT_ID.equals(o.getAlpacaAccountId())));
    }

    @Test
    void enqueue_sell_success() {
        PlaceOrderRequest request = buildRequest("TSLA", 3);
        given(marketsClient.getStockPrice("TSLA", TOKEN)).willReturn(price("200.00"));
        HoldingId holdingId = new HoldingId(USER_ID, "TSLA");
        given(holdingRepository.findById(holdingId)).willReturn(Optional.of(new Holding(holdingId, 5, new BigDecimal("180.00"))));
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        QueuedOrderResponse result = service.enqueueOrder(request, USER_ID, ACCOUNT_ID, "sell", TOKEN, null);

        assertThat(result.status()).isEqualTo("PENDING_QUEUE");
        assertThat(result.side()).isEqualTo("sell");
        verify(orderRepository).save(any());
    }

    @Test
    void enqueue_buy_insufficientFunds() {
        PlaceOrderRequest request = buildRequest("AAPL", 10);
        given(marketsClient.getStockPrice("AAPL", TOKEN)).willReturn(price("9999.00"));
        given(accountRepository.findById(USER_ID)).willReturn(Optional.of(new Account(USER_ID, new BigDecimal("100.00"))));

        assertThatThrownBy(() -> service.enqueueOrder(request, USER_ID, ACCOUNT_ID, "buy", TOKEN, null))
                .isInstanceOf(InsufficientFundsException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void enqueue_sell_insufficientHoldings() {
        PlaceOrderRequest request = buildRequest("AAPL", 10);
        given(marketsClient.getStockPrice("AAPL", TOKEN)).willReturn(price("100.00"));
        HoldingId holdingId = new HoldingId(USER_ID, "AAPL");
        given(holdingRepository.findById(holdingId)).willReturn(Optional.of(new Holding(holdingId, 2, new BigDecimal("90.00"))));

        assertThatThrownBy(() -> service.enqueueOrder(request, USER_ID, ACCOUNT_ID, "sell", TOKEN, null))
                .isInstanceOf(InsufficientHoldingsException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void processQueue_skipsWhenMarketClosed() {
        given(alpacaClockClient.isMarketOpen()).willReturn(false);

        service.processQueue();

        verify(orderRepository, never()).findAllByStatusOrderByCreatedAtAsc(any());
        verify(orderService, never()).executeQueuedBuyOrder(any(), any());
    }

    @Test
    void processQueue_executesQueuedOrders() {
        Order pending = Order.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .symbol("AAPL")
                .side("buy")
                .orderType("market")
                .quantity(1)
                .estimatedPrice(new BigDecimal("150.00"))
                .alpacaAccountId(ACCOUNT_ID)
                .status(OrderStatus.PENDING_QUEUE)
                .createdAt(Instant.now())
                .build();

        given(alpacaClockClient.isMarketOpen()).willReturn(true);
        given(orderRepository.findAllByStatusOrderByCreatedAtAsc(OrderStatus.PENDING_QUEUE))
                .willReturn(List.of(pending));

        service.processQueue();

        verify(orderService).executeQueuedBuyOrder(pending, ACCOUNT_ID);
    }

    private PlaceOrderRequest buildRequest(String symbol, int qty) {
        PlaceOrderRequest r = new PlaceOrderRequest();
        setField(r, "symbol", symbol);
        setField(r, "quantity", qty);
        setField(r, "orderType", "market");
        return r;
    }

    private StockPriceDto price(String val) {
        StockPriceDto dto = new StockPriceDto();
        setField(dto, "price", new BigDecimal(val));
        return dto;
    }

    private void setField(Object obj, String name, Object value) {
        try {
            var field = findField(obj.getClass(), name);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) return findField(clazz.getSuperclass(), name);
            throw e;
        }
    }
}
