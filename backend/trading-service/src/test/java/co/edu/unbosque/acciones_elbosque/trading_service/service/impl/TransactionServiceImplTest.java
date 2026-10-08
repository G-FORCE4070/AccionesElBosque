package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.TransactionHistoryResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.TransactionItem;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Order;
import co.edu.unbosque.acciones_elbosque.trading_service.model.OrderStatus;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void getTransactions_onlyReturnsExecutedOrders() {
        Order executed = buildOrder("buy", new BigDecimal("100.00"), new BigDecimal("1.00"));
        Page<Order> page = new PageImpl<>(List.of(executed), PageRequest.of(0, 20), 1);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        TransactionHistoryResponse response = transactionService.getTransactions(USER_ID, null, null, null, null, 0, 20);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void getTransactions_buyOrder_totalIsNegative() {
        // BUY: total = -(subtotal + commission)
        Order buy = buildOrder("buy", new BigDecimal("100.00"), new BigDecimal("1.00"));
        Page<Order> page = new PageImpl<>(List.of(buy), PageRequest.of(0, 20), 1);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        TransactionHistoryResponse response = transactionService.getTransactions(USER_ID, "buy", null, null, null, 0, 20);

        TransactionItem item = response.content().get(0);
        assertThat(item.subtotal()).isEqualByComparingTo(new BigDecimal("500.00")); // 100 * 5
        assertThat(item.total()).isEqualByComparingTo(new BigDecimal("-501.00")); // -(500 + 1)
    }

    @Test
    void getTransactions_sellOrder_totalIsPositive() {
        // SELL: total = subtotal - commission
        Order sell = buildOrder("sell", new BigDecimal("200.00"), new BigDecimal("2.00"));
        Page<Order> page = new PageImpl<>(List.of(sell), PageRequest.of(0, 20), 1);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        TransactionHistoryResponse response = transactionService.getTransactions(USER_ID, "sell", null, null, null, 0, 20);

        TransactionItem item = response.content().get(0);
        assertThat(item.subtotal()).isEqualByComparingTo(new BigDecimal("1000.00")); // 200 * 5
        assertThat(item.total()).isEqualByComparingTo(new BigDecimal("998.00")); // 1000 - 2
    }

    @Test
    void getTransactions_totalCommission_summedCorrectly() {
        Order o1 = buildOrder("buy", new BigDecimal("100.00"), new BigDecimal("1.50"));
        Order o2 = buildOrder("sell", new BigDecimal("200.00"), new BigDecimal("2.50"));
        Page<Order> page = new PageImpl<>(List.of(o1, o2), PageRequest.of(0, 20), 2);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        TransactionHistoryResponse response = transactionService.getTransactions(USER_ID, null, null, null, null, 0, 20);

        assertThat(response.totalCommission()).isEqualByComparingTo(new BigDecimal("4.00"));
    }

    @Test
    void getTransactions_emptyResult_returnsTotalCommissionZero() {
        Page<Order> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        TransactionHistoryResponse response = transactionService.getTransactions(USER_ID, null, null, null, null, 0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalCommission()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTransactions_filterBySymbol_returnsMappedItem() {
        Order order = buildOrder("buy", new BigDecimal("150.00"), new BigDecimal("1.50"));
        Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        TransactionHistoryResponse response = transactionService.getTransactions(USER_ID, null, "AAPL", null, null, 0, 20);

        assertThat(response.content().get(0).symbol()).isEqualTo("AAPL");
    }

    private Order buildOrder(String side, BigDecimal executedPrice, BigDecimal commission) {
        return Order.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .symbol("AAPL")
                .side(side)
                .orderType("market")
                .quantity(5)
                .executedPrice(executedPrice)
                .commission(commission)
                .status(OrderStatus.EXECUTED)
                .createdAt(Instant.now())
                .executedAt(Instant.now())
                .build();
    }
}
