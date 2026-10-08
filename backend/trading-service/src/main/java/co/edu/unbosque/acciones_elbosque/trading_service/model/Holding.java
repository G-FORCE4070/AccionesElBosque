package co.edu.unbosque.acciones_elbosque.trading_service.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "holdings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Holding {

    @EmbeddedId
    private HoldingId id;
    private int quantity;
    private BigDecimal avgCost;
    private Instant updatedAt;

    public Holding(HoldingId id, int quantity, BigDecimal avgCost) {
        this.id = id;
        this.quantity = quantity;
        this.avgCost = avgCost;
        this.updatedAt = Instant.now();
    }
}
