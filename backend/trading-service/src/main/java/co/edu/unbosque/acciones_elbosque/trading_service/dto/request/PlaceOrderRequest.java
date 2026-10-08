package co.edu.unbosque.acciones_elbosque.trading_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaceOrderRequest {

    @NotBlank
    private String symbol;

    @Min(1)
    private int quantity;

    @NotNull
    private String orderType;

    private boolean queue;
}
