package co.edu.unbosque.acciones_elbosque.markets_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalTime;

@Entity
@Table(name = "market_schedules")
@Getter
public class MarketSchedule {

    @Id
    private String marketCode;
    private String marketName;
    private LocalTime openTimeUtc;
    private LocalTime closeTimeUtc;
    private String timezone;
    private boolean active;
}
