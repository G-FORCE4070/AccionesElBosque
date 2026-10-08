package co.edu.unbosque.acciones_elbosque.identity_service.service;

public interface StripeCheckoutService {
    String createCheckoutSession(String customerEmail, String billingCycle);
    /** Returns the customer email if the session is paid; throws PaymentNotCompletedException otherwise. */
    String getCustomerEmailIfPaid(String sessionId);
}
