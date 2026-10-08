package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.CheckoutResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.ConfirmPaymentResponse;

public interface SubscriptionService {
    ConfirmPaymentResponse confirmPayment(String sessionId);
    CheckoutResponse checkout(String userId, String billingCycle);
    void cancel(String userId);
}
