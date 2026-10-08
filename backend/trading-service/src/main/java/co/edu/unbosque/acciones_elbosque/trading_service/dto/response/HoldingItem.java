package co.edu.unbosque.acciones_elbosque.trading_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class HoldingItem {

    private String symbol;
    private int quantity;
    private BigDecimal avgCost;
    private BigDecimal currentPrice;
    private BigDecimal totalValue;
    private BigDecimal gainPct;
    private BigDecimal gainDollars;
}
