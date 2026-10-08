package co.edu.unbosque.acciones_elbosque.trading_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class OrderResponse {

    private String orderId;
    private String symbol;
    private String side;
    private String orderType;
    private int quantity;
    private BigDecimal estimatedPrice;
    private BigDecimal executedPrice;
    private BigDecimal commission;
    private BigDecimal totalAmount;
    private BigDecimal newBalance;
    private String status;
    private Instant executedAt;
}
