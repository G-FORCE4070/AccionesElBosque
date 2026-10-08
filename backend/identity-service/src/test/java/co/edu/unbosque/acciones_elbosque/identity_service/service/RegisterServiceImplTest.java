package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.RegisterRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.RegisterResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.AlpacaAccountCreationException;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.EmailAlreadyRegisteredException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.model.UserPreference;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserPreferenceRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.impl.RegisterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserPreferenceRepository preferenceRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;
    @Mock private AuditService auditService;
    @Mock private AlpacaBrokerService alpacaBrokerService;
    @Mock private StripeCheckoutService stripeCheckoutService;

    @InjectMocks private RegisterServiceImpl registerService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setFirstName("María");
        validRequest.setLastName("Clavijo");
        validRequest.setEmail("maria@ejemplo.com");
        validRequest.setPhone("+57300000000");
        validRequest.setPassword("Password1!");
        validRequest.setBirthDate("1990-01-01");
        validRequest.setDocType("cc");
        validRequest.setDocNumber("12345678");
        validRequest.setNationality("Colombia");
        validRequest.setCountry("CO");
        validRequest.setAddress("Calle 123");
        validRequest.setCity("Bogotá");
        validRequest.setPostalCode("110111");
        validRequest.setIncomeSource("salary");
        validRequest.setAnnualIncome("30k-100k");
        validRequest.setNetWorth("50k-250k");
        validRequest.setRiskTolerance("moderate");
        validRequest.setTradingExperience("beginner");
        validRequest.setInvestmentGoal("growth");
        validRequest.setIsPep(false);
        validRequest.setIsFinraAffiliated(false);
        validRequest.setAcceptsTerms(true);
        validRequest.setAcceptsPrivacy(true);
        validRequest.setAuthorizeDataUse(true);
        validRequest.setMarkets(List.of("nyse", "nasdaq"));
        validRequest.setSectors(List.of("tech"));
        validRequest.setPlan("basic");
    }

    @Test
    void register_basicPlan_success() {
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("maria@ejemplo.com")
                .firstName("María")
                .build();
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(preferenceRepository.save(any())).thenReturn(new UserPreference());
        when(alpacaBrokerService.createAccount(any())).thenReturn("alpaca-123");
        doNothing().when(alpacaBrokerService).fundAccount(any());

        RegisterResponse result = registerService.register(validRequest);

        assertThat(result.getMessage()).isEqualTo("Registro exitoso. Revisa tu correo de bienvenida.");
        assertThat(result.getCheckoutUrl()).isNull();
        verify(alpacaBrokerService).fundAccount("alpaca-123");
        verify(stripeCheckoutService, never()).createCheckoutSession(any(), any());
        verify(auditService).sendEvent(eq("USER_REGISTERED"), any(), any());
        verify(auditService).sendEvent(eq("ALPACA_ACCOUNT_CREATED"), any(), any());
    }

    @Test
    void register_premiumPlan_success() {
        validRequest.setPlan("premium");
        validRequest.setBillingCycle("monthly");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("maria@ejemplo.com")
                .firstName("María")
                .build();
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(preferenceRepository.save(any())).thenReturn(new UserPreference());
        when(alpacaBrokerService.createAccount(any())).thenReturn("alpaca-123");
        doNothing().when(alpacaBrokerService).fundAccount(any());
        when(stripeCheckoutService.createCheckoutSession(any(), eq("monthly")))
                .thenReturn("https://checkout.stripe.com/pay/cs_test_123");

        RegisterResponse result = registerService.register(validRequest);

        assertThat(result.getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_123");
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyRegisteredException() {
        when(userRepository.existsByEmail("maria@ejemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> registerService.register(validRequest))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("El correo electrónico ya está registrado");

        verify(alpacaBrokerService, never()).createAccount(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_alpacaFailure_throwsAlpacaAccountCreationException() {
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("maria@ejemplo.com")
                .firstName("María")
                .build();
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(preferenceRepository.save(any())).thenReturn(new UserPreference());
        when(alpacaBrokerService.createAccount(any()))
                .thenThrow(new AlpacaAccountCreationException("Alpaca error"));

        assertThatThrownBy(() -> registerService.register(validRequest))
                .isInstanceOf(AlpacaAccountCreationException.class);

        verify(auditService, never()).sendEvent(any(), any(), any());
        verify(stripeCheckoutService, never()).createCheckoutSession(any(), any());
    }
}
