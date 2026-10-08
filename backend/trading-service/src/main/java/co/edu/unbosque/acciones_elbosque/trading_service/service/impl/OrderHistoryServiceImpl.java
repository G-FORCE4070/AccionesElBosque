package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.OrderHistoryItem;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.OrderHistoryResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Order;
import co.edu.unbosque.acciones_elbosque.trading_service.model.OrderStatus;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.OrderRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.service.OrderHistoryService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderHistoryServiceImpl implements OrderHistoryService {

    private final OrderRepository orderRepository;

    public OrderHistoryServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderHistoryResponse getOrders(UUID userId, String symbol, String status,
                                          LocalDate startDate, LocalDate endDate,
                                          int page, int size) {
        Specification<Order> spec = buildSpec(userId, symbol, status, startDate, endDate);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = orderRepository.findAll(spec, pageable);

        List<OrderHistoryItem> items = result.getContent().stream()
                .map(this::toItem)
                .toList();

        return new OrderHistoryResponse(
                items,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    private Specification<Order> buildSpec(UUID userId, String symbol, String status,
                                           LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));

            if (symbol != null && !symbol.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("symbol")), symbol.toUpperCase()));
            }
            if (status != null && !status.isBlank()) {
                try {
                    OrderStatus s = OrderStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), s));
                } catch (IllegalArgumentException ignored) {
                    // invalid status value — no filter applied
                }
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), startDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(
                        root.get("createdAt"), endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private OrderHistoryItem toItem(Order o) {
        return new OrderHistoryItem(
                o.getId().toString(),
                o.getSymbol(),
                o.getSide(),
                o.getOrderType(),
                o.getQuantity(),
                o.getExecutedPrice(),
                o.getCommission(),
                o.getStatus().name(),
                o.getCreatedAt(),
                o.getExecutedAt()
        );
    }
}
