package co.edu.unbosque.acciones_elbosque.markets_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AlpacaClockDto {

    @JsonProperty("is_open")
    private boolean open;
}
