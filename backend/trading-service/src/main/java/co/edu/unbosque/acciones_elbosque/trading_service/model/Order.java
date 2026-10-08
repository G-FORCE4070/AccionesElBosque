package co.edu.unbosque.acciones_elbosque.trading_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private UUID id;
    private UUID userId;
    private String symbol;
    private String side;
    private String orderType;
    private int quantity;
    private BigDecimal estimatedPrice;
    private BigDecimal executedPrice;
    private BigDecimal commission;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String alpacaOrderId;
    private String alpacaAccountId;
    private String userEmail;
    private Instant createdAt;
    private Instant executedAt;
}
