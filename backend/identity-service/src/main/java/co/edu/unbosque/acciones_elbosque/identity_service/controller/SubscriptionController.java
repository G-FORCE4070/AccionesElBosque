package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.CheckoutRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.ConfirmPaymentRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.CheckoutResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.ConfirmPaymentResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmPaymentResponse> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(subscriptionService.confirmPayment(request.getSessionId()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(subscriptionService.checkout(userId, request.getBillingCycle()));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        subscriptionService.cancel(userId);
        return ResponseEntity.ok().build();
    }
}
