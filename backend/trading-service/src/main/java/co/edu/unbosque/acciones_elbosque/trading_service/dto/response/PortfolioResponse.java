package co.edu.unbosque.acciones_elbosque.trading_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PortfolioResponse {

    private BigDecimal balance;
    private boolean pricesAvailable;
    private List<HoldingItem> holdings;
}
