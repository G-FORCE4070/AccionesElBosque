package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.client.AlpacaBrokerClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.AuditClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.MarketsServiceClient;
import co.edu.unbosque.acciones_elbosque.trading_service.client.NotificationClient;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.AlpacaOrderResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.MarketStatusDto;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.StockPriceDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock AccountRepository accountRepository;
    @Mock HoldingRepository holdingRepository;
    @Mock OrderRepository orderRepository;
    @Mock MarketsServiceClient marketsClient;
    @Mock AlpacaBrokerClient alpacaClient;
    @Mock AuditClient auditClient;
    @Mock NotificationClient notificationClient;

    OrderServiceImpl service;

    static final UUID USER_ID = UUID.randomUUID();
    static final String ACCOUNT_ID = "alpaca-acc-1";
    static final String TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                accountRepository, holdingRepository, orderRepository,
                marketsClient, alpacaClient, auditClient, notificationClient,
                new BigDecimal("0.02")
        );
    }

    private MarketStatusDto openMarket() {
        MarketStatusDto dto = new MarketStatusDto();
        setField(dto, "open", true);
        setField(dto, "symbol", "AAPL");
        return dto;
    }

    private MarketStatusDto closedMarket() {
        MarketStatusDto dto = new MarketStatusDto();
        setField(dto, "open", false);
        return dto;
    }

    private StockPriceDto stockPrice(String price) {
        StockPriceDto dto = new StockPriceDto();
        setField(dto, "price", new BigDecimal(price));
        return dto;
    }

    private AlpacaOrderResponse filledOrder() {
        AlpacaOrderResponse resp = new AlpacaOrderResponse();
        setField(resp, "id", "alpaca-order-1");
        setField(resp, "status", "filled");
        setField(resp, "filledAvgPrice", new BigDecimal("150.00"));
        return resp;
    }

    private PlaceOrderRequest buildRequest(String symbol, int qty) {
        PlaceOrderRequest req = new PlaceOrderRequest();
        setField(req, "symbol", symbol);
        setField(req, "quantity", qty);
        setField(req, "orderType", "market");
        return req;
    }

    private Account accountWith(String balance) {
        return new Account(USER_ID, new BigDecimal(balance), Instant.now());
    }

    @SuppressWarnings("unchecked")
    private <T> void setField(Object obj, String name, Object value) {
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

    private Order savedOrder(String side) {
        return Order.builder()
                .id(UUID.randomUUID()).userId(USER_ID).symbol("AAPL")
                .side(side).orderType("market").quantity(1)
                .estimatedPrice(new BigDecimal("150.00"))
                .status(OrderStatus.SENT).createdAt(Instant.now()).build();
    }

    @Test
    void buyOrder_success_returnsExecutedOrder() {
        when(marketsClient.getMarketStatus("AAPL", TOKEN)).thenReturn(openMarket());
        when(marketsClient.getStockPrice("AAPL", TOKEN)).thenReturn(stockPrice("150.00"));
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(accountWith("50000.00")));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(alpacaClient.placeOrder(anyString(), any())).thenReturn(filledOrder());
        when(alpacaClient.getOrderStatus(anyString(), anyString())).thenReturn(filledOrder());
        when(holdingRepository.findById(any())).thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditClient).sendEvent(any());
        doNothing().when(notificationClient).notifyOrderExecuted(any());

        OrderResponse resp = service.placeBuyOrder(buildRequest("AAPL", 1), USER_ID, ACCOUNT_ID, TOKEN, null);

        assertThat(resp.getSide()).isEqualTo("buy");
        assertThat(resp.getStatus()).isEqualTo("EXECUTED");
        assertThat(resp.getExecutedPrice()).isEqualByComparingTo("150.00");
    }

    @Test
    void buyOrder_marketClosed_throwsMarketClosedException() {
        when(marketsClient.getMarketStatus("AAPL", TOKEN)).thenReturn(closedMarket());

        assertThatThrownBy(() -> service.placeBuyOrder(buildRequest("AAPL", 1), USER_ID, ACCOUNT_ID, TOKEN, null))
                .isInstanceOf(MarketClosedException.class);
    }

    @Test
    void buyOrder_insufficientFunds_throwsException() {
        when(marketsClient.getMarketStatus("AAPL", TOKEN)).thenReturn(openMarket());
        when(marketsClient.getStockPrice("AAPL", TOKEN)).thenReturn(stockPrice("150.00"));
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(accountWith("10.00")));

        assertThatThrownBy(() -> service.placeBuyOrder(buildRequest("AAPL", 1), USER_ID, ACCOUNT_ID, TOKEN, null))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void buyOrder_existingHolding_updatesAvgCost() {
        when(marketsClient.getMarketStatus("AAPL", TOKEN)).thenReturn(openMarket());
        when(marketsClient.getStockPrice("AAPL", TOKEN)).thenReturn(stockPrice("150.00"));
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(accountWith("50000.00")));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(alpacaClient.placeOrder(anyString(), any())).thenReturn(filledOrder());
        when(alpacaClient.getOrderStatus(anyString(), anyString())).thenReturn(filledOrder());

        HoldingId holdingId = new HoldingId(USER_ID, "AAPL");
        Holding existing = new Holding(holdingId, 10, new BigDecimal("100.00"), Instant.now());
        when(holdingRepository.findById(any())).thenReturn(Optional.of(existing));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditClient).sendEvent(any());
        doNothing().when(notificationClient).notifyOrderExecuted(any());

        service.placeBuyOrder(buildRequest("AAPL", 10), USER_ID, ACCOUNT_ID, TOKEN, null);

        assertThat(existing.getQuantity()).isEqualTo(20);
        assertThat(existing.getAvgCost()).isEqualByComparingTo("125.00");
    }

    @Test
    void sellOrder_success_returnsExecutedOrder() {
        when(marketsClient.getMarketStatus("AAPL", TOKEN)).thenReturn(openMarket());
        when(marketsClient.getStockPrice("AAPL", TOKEN)).thenReturn(stockPrice("150.00"));
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(accountWith("50000.00")));

        HoldingId holdingId = new HoldingId(USER_ID, "AAPL");
        Holding holding = new Holding(holdingId, 5, new BigDecimal("100.00"), Instant.now());
        when(holdingRepository.findById(any())).thenReturn(Optional.of(holding));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(alpacaClient.placeOrder(anyString(), any())).thenReturn(filledOrder());
        when(alpacaClient.getOrderStatus(anyString(), anyString())).thenReturn(filledOrder());
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditClient).sendEvent(any());
        doNothing().when(notificationClient).notifyOrderExecuted(any());

        OrderResponse resp = service.placeSellOrder(buildRequest("AAPL", 2), USER_ID, ACCOUNT_ID, TOKEN, null);

        assertThat(resp.getSide()).isEqualTo("sell");
        assertThat(resp.getStatus()).isEqualTo("EXECUTED");
    }

    @Test
    void sellOrder_noHolding_throwsInsufficientHoldings() {
        when(marketsClient.getMarketStatus("AAPL", TOKEN)).thenReturn(openMarket());
        when(marketsClient.getStockPrice("AAPL", TOKEN)).thenReturn(stockPrice("150.00"));
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(accountWith("50000.00")));
        when(holdingRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.placeSellOrder(buildRequest("AAPL", 1), USER_ID, ACCOUNT_ID, TOKEN, null))
                .isInstanceOf(InsufficientHoldingsException.class);
    }

    @Test
    void sellOrder_insufficientQuantity_throwsException() {
        when(marketsClient.getMarketStatus("AAPL", TOKEN)).thenReturn(openMarket());
        when(marketsClient.getStockPrice("AAPL", TOKEN)).thenReturn(stockPrice("150.00"));
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(accountWith("50000.00")));

        HoldingId holdingId = new HoldingId(USER_ID, "AAPL");
        Holding holding = new Holding(holdingId, 1, new BigDecimal("100.00"), Instant.now());
        when(holdingRepository.findById(any())).thenReturn(Optional.of(holding));

        assertThatThrownBy(() -> service.placeSellOrder(buildRequest("AAPL", 5), USER_ID, ACCOUNT_ID, TOKEN, null))
                .isInstanceOf(InsufficientHoldingsException.class);
    }

    @Test
    void sellOrder_exactQuantity_deletesHolding() {
        when(marketsClient.getMarketStatus("AAPL", TOKEN)).thenReturn(openMarket());
        when(marketsClient.getStockPrice("AAPL", TOKEN)).thenReturn(stockPrice("150.00"));
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(accountWith("50000.00")));

        HoldingId holdingId = new HoldingId(USER_ID, "AAPL");
        Holding holding = new Holding(holdingId, 3, new BigDecimal("100.00"), Instant.now());
        when(holdingRepository.findById(any())).thenReturn(Optional.of(holding));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(alpacaClient.placeOrder(anyString(), any())).thenReturn(filledOrder());
        when(alpacaClient.getOrderStatus(anyString(), anyString())).thenReturn(filledOrder());
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(holdingRepository).delete(any());
        doNothing().when(auditClient).sendEvent(any());
        doNothing().when(notificationClient).notifyOrderExecuted(any());

        service.placeSellOrder(buildRequest("AAPL", 3), USER_ID, ACCOUNT_ID, TOKEN, null);

        org.mockito.Mockito.verify(holdingRepository).delete(holding);
    }
}
