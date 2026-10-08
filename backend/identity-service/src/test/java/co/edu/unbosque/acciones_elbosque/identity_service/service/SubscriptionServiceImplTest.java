package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.CheckoutResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.UserNotFoundException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private StripeCheckoutService stripeCheckoutService;
    @Mock private AuditService auditService;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@test.com")
                .passwordHash("hash")
                .firstName("Ana")
                .lastName("Gómez")
                .phone("+57300000000")
                .birthDate(LocalDate.of(1990, 1, 1))
                .docType("cc")
                .docNumber("12345678")
                .nationality("CO")
                .country("CO")
                .address("Calle 1")
                .city("Bogotá")
                .postalCode("110111")
                .incomeSource("salary")
                .annualIncome("30k-100k")
                .netWorth("50k-250k")
                .riskTolerance("moderate")
                .tradingExperience("beginner")
                .investmentGoal("growth")
                .plan("BASIC")
                .build();
    }

    @Test
    void checkout_validUser_returnsCheckoutUrl() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stripeCheckoutService.createCheckoutSession(eq("test@test.com"), eq("monthly")))
                .thenReturn("https://checkout.stripe.com/test");

        CheckoutResponse response = subscriptionService.checkout(userId.toString(), "monthly");

        assertThat(response.getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/test");
        verify(auditService).sendEvent(eq("SUBSCRIPTION_CREATED"), eq(userId.toString()), any());
    }

    @Test
    void checkout_stripeNotConfigured_returnsNullUrl() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stripeCheckoutService.createCheckoutSession(any(), any())).thenReturn(null);

        CheckoutResponse response = subscriptionService.checkout(userId.toString(), "annual");

        assertThat(response.getCheckoutUrl()).isNull();
    }

    @Test
    void checkout_unknownUser_throwsUserNotFoundException() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.checkout(UUID.randomUUID().toString(), "monthly"))
                .isInstanceOf(UserNotFoundException.class);

        verify(stripeCheckoutService, never()).createCheckoutSession(any(), any());
    }

    @Test
    void cancel_premiumUser_setsBasicAndAudits() {
        user.setPlan("PREMIUM");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        subscriptionService.cancel(userId.toString());

        assertThat(user.getPlan()).isEqualTo("BASIC");
        verify(userRepository).save(user);
        verify(auditService).sendEvent(eq("SUBSCRIPTION_CANCELED"), eq(userId.toString()), any());
        verify(auditService).sendEvent(eq("PLAN_CHANGED"), eq(userId.toString()), any());
    }

    @Test
    void cancel_unknownUser_throwsUserNotFoundException() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.cancel(UUID.randomUUID().toString()))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }
}
