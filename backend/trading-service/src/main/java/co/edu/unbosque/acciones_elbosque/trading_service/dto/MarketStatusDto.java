package co.edu.unbosque.acciones_elbosque.trading_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MarketStatusDto {

    private String symbol;
    private String market;
    private boolean open;
    private String opensAt;
    private String closesAt;
}
