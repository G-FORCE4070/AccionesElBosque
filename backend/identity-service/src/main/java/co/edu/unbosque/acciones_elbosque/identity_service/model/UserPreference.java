package co.edu.unbosque.acciones_elbosque.identity_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "market_open")
    @Builder.Default
    private boolean marketOpen = false;

    @Column(name = "price_alerts")
    @Builder.Default
    private boolean priceAlerts = false;

    @Column(name = "daily_summary")
    @Builder.Default
    private boolean dailySummary = false;

    @Column(name = "order_confirm")
    @Builder.Default
    private boolean orderConfirm = true;

    @Column(name = "notif_email")
    @Builder.Default
    private boolean notifEmail = true;

    @Column(name = "notif_sms")
    @Builder.Default
    private boolean notifSms = false;

    @Column(name = "notif_whatsapp")
    @Builder.Default
    private boolean notifWhatsapp = false;

    @Column(name = "monthly_report")
    @Builder.Default
    private boolean monthlyReport = false;

    @Column(name = "view_type", length = 20)
    @Builder.Default
    private String viewType = "list";

    @Column(name = "default_order", length = 20)
    @Builder.Default
    private String defaultOrder = "totalValue";

    @Column(name = "show_low_weight")
    @Builder.Default
    private boolean showLowWeight = true;

    @Column(name = "default_order_type", length = 20)
    @Builder.Default
    private String defaultOrderType = "market";

    @Column(name = "daily_limit")
    @Builder.Default
    private int dailyLimit = 0;

    @Column(name = "require_double_auth")
    @Builder.Default
    private boolean requireDoubleAuth = false;

    @Column(length = 255)
    private String markets;

    @Column(length = 255)
    private String sectors;
}
