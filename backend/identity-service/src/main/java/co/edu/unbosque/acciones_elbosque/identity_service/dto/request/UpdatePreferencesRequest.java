package co.edu.unbosque.acciones_elbosque.identity_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePreferencesRequest {

    private Boolean marketOpen;
    private Boolean priceAlerts;
    private Boolean dailySummary;
    private Boolean orderConfirm;

    private Boolean notifEmail;
    private Boolean notifSms;
    private Boolean notifWhatsapp;
    private Boolean monthlyReport;

    @Pattern(regexp = "list|bars|detail", message = "viewType debe ser list, bars o detail")
    private String viewType;

    @Pattern(regexp = "totalValue|gainPct|symbol", message = "defaultOrder debe ser totalValue, gainPct o symbol")
    private String defaultOrder;

    private Boolean showLowWeight;

    @Pattern(regexp = "market", message = "defaultOrderType solo acepta 'market' en el MVP")
    private String defaultOrderType;

    @Min(value = 0, message = "dailyLimit debe ser mayor o igual a 0")
    private Integer dailyLimit;

    private Boolean requireDoubleAuth;

    @Pattern(regexp = "nyse|nasdaq", message = "market debe ser nyse o nasdaq")
    private String market;

    @Pattern(regexp = "tech|health|consumer", message = "sector debe ser tech, health o consumer")
    private String sector;
}
