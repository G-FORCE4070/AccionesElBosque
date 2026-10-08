package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.TransactionHistoryResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.TransactionItem;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Order;
import co.edu.unbosque.acciones_elbosque.trading_service.model.OrderStatus;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.OrderRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.service.TransactionService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final OrderRepository orderRepository;

    public TransactionServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public TransactionHistoryResponse getTransactions(UUID userId, String type, String symbol,
                                                      LocalDate startDate, LocalDate endDate,
                                                      int page, int size) {
        Specification<Order> spec = buildSpec(userId, type, symbol, startDate, endDate);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executedAt"));
        Page<Order> result = orderRepository.findAll(spec, pageable);

        List<TransactionItem> items = result.getContent().stream()
                .map(this::toItem)
                .toList();

        BigDecimal totalCommission = items.stream()
                .map(TransactionItem::commission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionHistoryResponse(
                items,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                totalCommission
        );
    }

    private Specification<Order> buildSpec(UUID userId, String type, String symbol,
                                           LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("status"), OrderStatus.EXECUTED));

            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("side")), type.toLowerCase()));
            }
            if (symbol != null && !symbol.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("symbol")), symbol.toUpperCase()));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("executedAt"), startDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(
                        root.get("executedAt"), endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TransactionItem toItem(Order o) {
        BigDecimal subtotal = o.getExecutedPrice().multiply(BigDecimal.valueOf(o.getQuantity()));
        BigDecimal commission = o.getCommission() != null ? o.getCommission() : BigDecimal.ZERO;
        // BUY: money leaves the account (negative); SELL: money enters (positive)
        BigDecimal total = "buy".equalsIgnoreCase(o.getSide())
                ? subtotal.add(commission).negate()
                : subtotal.subtract(commission);

        return new TransactionItem(
                o.getId().toString(),
                o.getExecutedAt(),
                o.getSide(),
                o.getSymbol(),
                o.getQuantity(),
                o.getExecutedPrice(),
                subtotal,
                commission,
                total
        );
    }
}
