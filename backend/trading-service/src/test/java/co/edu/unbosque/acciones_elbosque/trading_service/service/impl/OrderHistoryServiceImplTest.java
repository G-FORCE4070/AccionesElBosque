package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.OrderHistoryResponse;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderHistoryServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderHistoryServiceImpl orderHistoryService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void getOrders_noFilters_returnsPaginatedList() {
        Order order = buildOrder(OrderStatus.EXECUTED);
        Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        OrderHistoryResponse response = orderHistoryService.getOrders(USER_ID, null, null, null, null, 0, 20);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).symbol()).isEqualTo("AAPL");
        assertThat(response.content().get(0).status()).isEqualTo("EXECUTED");
    }

    @Test
    void getOrders_filterBySymbol_mapsSymbolCorrectly() {
        Order order = buildOrder(OrderStatus.SENT);
        Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        OrderHistoryResponse response = orderHistoryService.getOrders(USER_ID, "AAPL", null, null, null, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).symbol()).isEqualTo("AAPL");
        assertThat(response.content().get(0).status()).isEqualTo("SENT");
    }

    @Test
    void getOrders_filterByStatus_mapsStatusCorrectly() {
        Order order = buildOrder(OrderStatus.PENDING_QUEUE);
        Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        OrderHistoryResponse response = orderHistoryService.getOrders(USER_ID, null, "PENDING_QUEUE", null, null, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo("PENDING_QUEUE");
    }

    @Test
    void getOrders_filterByDateRange_returnsMappedResponse() {
        Order order = buildOrder(OrderStatus.EXECUTED);
        Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);
        OrderHistoryResponse response = orderHistoryService.getOrders(USER_ID, null, null, start, end, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).executedPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.content().get(0).commission()).isEqualByComparingTo(new BigDecimal("1.50"));
    }

    @Test
    void getOrders_noOrders_returnsEmptyPage() {
        Page<Order> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        given(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        OrderHistoryResponse response = orderHistoryService.getOrders(USER_ID, null, null, null, null, 0, 20);

        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.content()).isEmpty();
    }

    private Order buildOrder(OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .symbol("AAPL")
                .side("buy")
                .orderType("market")
                .quantity(5)
                .executedPrice(new BigDecimal("150.00"))
                .commission(new BigDecimal("1.50"))
                .status(status)
                .createdAt(Instant.now())
                .executedAt(status == OrderStatus.EXECUTED ? Instant.now() : null)
                .build();
    }
}
