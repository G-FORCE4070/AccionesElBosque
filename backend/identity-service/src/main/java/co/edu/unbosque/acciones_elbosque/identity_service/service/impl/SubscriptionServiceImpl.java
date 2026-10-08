package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.CheckoutResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.ConfirmPaymentResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.UserNotFoundException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.StripeCheckoutService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final StripeCheckoutService stripeCheckoutService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ConfirmPaymentResponse confirmPayment(String sessionId) {
        String email = stripeCheckoutService.getCustomerEmailIfPaid(sessionId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado para el correo: " + email));

        if ("PREMIUM".equals(user.getPlan())) {
            log.info("Plan already PREMIUM for user: {}", email);
            return new ConfirmPaymentResponse("Tu cuenta Premium ya estaba activa.", "PREMIUM");
        }

        user.setPlan("PREMIUM");
        userRepository.save(user);
        log.info("Plan upgraded to PREMIUM for user: {}", email);

        auditService.sendEvent("PLAN_CHANGED", user.getId().toString(),
                Map.of("email", email, "plan", "PREMIUM", "sessionId", sessionId));

        return new ConfirmPaymentResponse("¡Pago confirmado! Tu cuenta Premium está activa.", "PREMIUM");
    }

    @Override
    @Transactional
    public CheckoutResponse checkout(String userId, String billingCycle) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(UserNotFoundException::new);

        String checkoutUrl = stripeCheckoutService.createCheckoutSession(user.getEmail(), billingCycle);
        log.info("Checkout session created for userId={}, billingCycle={}", userId, billingCycle);

        auditService.sendEvent("SUBSCRIPTION_CREATED", userId,
                Map.of("email", user.getEmail(), "billingCycle", billingCycle));

        return new CheckoutResponse(checkoutUrl);
    }

    @Override
    @Transactional
    public void cancel(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(UserNotFoundException::new);

        user.setPlan("BASIC");
        userRepository.save(user);
        log.info("Subscription canceled for userId={}", userId);

        auditService.sendEvent("SUBSCRIPTION_CANCELED", userId, Map.of("email", user.getEmail()));
        auditService.sendEvent("PLAN_CHANGED", userId,
                Map.of("email", user.getEmail(), "plan", "BASIC"));
    }
}
