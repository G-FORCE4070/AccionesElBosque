package co.edu.unbosque.acciones_elbosque.identity_service.dto.response;

import co.edu.unbosque.acciones_elbosque.identity_service.model.UserPreference;
import lombok.Getter;

@Getter
public class PreferencesResponse {

    private final boolean marketOpen;
    private final boolean priceAlerts;
    private final boolean dailySummary;
    private final boolean orderConfirm;
    private final boolean notifEmail;
    private final boolean notifSms;
    private final boolean notifWhatsapp;
    private final boolean monthlyReport;
    private final String viewType;
    private final String defaultOrder;
    private final boolean showLowWeight;
    private final String defaultOrderType;
    private final int dailyLimit;
    private final boolean requireDoubleAuth;
    private final String market;
    private final String sector;

    public PreferencesResponse(UserPreference pref) {
        this.marketOpen = pref.isMarketOpen();
        this.priceAlerts = pref.isPriceAlerts();
        this.dailySummary = pref.isDailySummary();
        this.orderConfirm = pref.isOrderConfirm();
        this.notifEmail = pref.isNotifEmail();
        this.notifSms = pref.isNotifSms();
        this.notifWhatsapp = pref.isNotifWhatsapp();
        this.monthlyReport = pref.isMonthlyReport();
        this.viewType = pref.getViewType();
        this.defaultOrder = pref.getDefaultOrder();
        this.showLowWeight = pref.isShowLowWeight();
        this.defaultOrderType = pref.getDefaultOrderType();
        this.dailyLimit = pref.getDailyLimit();
        this.requireDoubleAuth = pref.isRequireDoubleAuth();
        this.market = pref.getMarkets();
        this.sector = pref.getSectors();
    }
}
