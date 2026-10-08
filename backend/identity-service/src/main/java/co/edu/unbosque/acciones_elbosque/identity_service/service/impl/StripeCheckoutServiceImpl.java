package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.exception.PaymentNotCompletedException;
import co.edu.unbosque.acciones_elbosque.identity_service.service.StripeCheckoutService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StripeCheckoutServiceImpl implements StripeCheckoutService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.price-id.monthly}")
    private String priceMonthly;

    @Value("${stripe.price-id.annual}")
    private String priceAnnual;

    @Value("${stripe.checkout.success-url}")
    private String successUrl;

    @Value("${stripe.checkout.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    public void validateConfig() {
        if ("sk_test_placeholder".equals(secretKey)) {
            log.warn("STRIPE_SECRET_KEY is a placeholder — Stripe sessions will fail. Set a real sk_test_... key.");
        }
        if ("price_monthly_placeholder".equals(priceMonthly) || "price_annual_placeholder".equals(priceAnnual)) {
            log.warn("Stripe price IDs are placeholders — set STRIPE_PREMIUM_MONTHLY_PRICE_ID and STRIPE_PREMIUM_ANNUAL_PRICE_ID.");
        } else {
            log.info("Stripe configured: monthly={}, annual={}", priceMonthly, priceAnnual);
        }
    }

    @Override
    public String createCheckoutSession(String customerEmail, String billingCycle) {
        if ("sk_test_placeholder".equals(secretKey)) {
            log.warn("Stripe not configured — skipping checkout session (STRIPE_SECRET_KEY is placeholder)");
            return null;
        }
        String priceId = "annual".equals(billingCycle) ? priceAnnual : priceMonthly;
        log.debug("Creating Stripe checkout session: email={}, billingCycle={}, priceId={}", customerEmail, billingCycle, priceId);
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomerEmail(customerEmail)
                    .setClientReferenceId(customerEmail)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .build();
            Session session = Session.create(params);
            log.info("Stripe checkout session created: sessionId={}", session.getId());
            return session.getUrl();
        } catch (StripeException ex) {
            log.error("Stripe checkout failed [code={}, status={}]: {}", ex.getCode(), ex.getStatusCode(), ex.getMessage());
            return null;
        }
    }

    @Override
    public String getCustomerEmailIfPaid(String sessionId) {
        try {
            SessionRetrieveParams params = SessionRetrieveParams.builder().build();
            Session session = Session.retrieve(sessionId, params, null);
            log.debug("Stripe session retrieved: id={}, status={}, paymentStatus={}", session.getId(), session.getStatus(), session.getPaymentStatus());
            if (!"paid".equals(session.getPaymentStatus())) {
                throw new PaymentNotCompletedException("El pago aún no ha sido procesado por Stripe.");
            }
            return session.getCustomerEmail();
        } catch (PaymentNotCompletedException ex) {
            throw ex;
        } catch (StripeException ex) {
            log.error("Stripe session retrieval failed [code={}, status={}]: {}", ex.getCode(), ex.getStatusCode(), ex.getMessage());
            throw new PaymentNotCompletedException("No se pudo verificar el pago con Stripe.");
        }
    }
}
