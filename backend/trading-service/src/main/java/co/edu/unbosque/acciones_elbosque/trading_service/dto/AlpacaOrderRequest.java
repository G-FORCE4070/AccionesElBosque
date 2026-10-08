package co.edu.unbosque.acciones_elbosque.trading_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlpacaOrderRequest {

    private String symbol;
    private String qty;
    private String side;
    private String type;

    @JsonProperty("time_in_force")
    private String timeInForce;
}
