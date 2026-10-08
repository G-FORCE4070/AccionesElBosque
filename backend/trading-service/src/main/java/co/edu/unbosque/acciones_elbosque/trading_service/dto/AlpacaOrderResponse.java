package co.edu.unbosque.acciones_elbosque.trading_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AlpacaOrderResponse {

    private String id;
    private String status;

    @JsonProperty("filled_avg_price")
    private BigDecimal filledAvgPrice;
}
