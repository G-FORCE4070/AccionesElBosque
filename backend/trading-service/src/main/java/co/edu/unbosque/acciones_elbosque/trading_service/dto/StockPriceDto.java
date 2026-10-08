package co.edu.unbosque.acciones_elbosque.trading_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class StockPriceDto {

    private String symbol;
    private String companyName;
    private String market;
    private String sector;
    private BigDecimal price;
    private BigDecimal change24h;
}
