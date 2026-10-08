package co.edu.unbosque.acciones_elbosque.trading_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    private UUID userId;
    private BigDecimal balance;
    private Instant updatedAt;

    public Account(UUID userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
        this.updatedAt = Instant.now();
    }
}
