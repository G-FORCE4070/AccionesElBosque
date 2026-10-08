package co.edu.unbosque.acciones_elbosque.markets_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "stocks")
@Getter
public class Stock {

    @Id
    private String symbol;
    private String companyName;
    private String marketCode;
    private String sector;
    private boolean active;
}
