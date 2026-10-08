package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.LoginRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.LoginResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.InvalidCredentialsException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.MfaEntry;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.impl.LoginServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;
    @Mock private CacheManager cacheManager;
    @Mock private AuditService auditService;
    @Mock private Cache sessionsCache;
    @Mock private Cache mfaCodesCache;

    @InjectMocks private LoginServiceImpl loginService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginService, "mailFrom", "noreply@test.com");
    }

    @Test
    void login_success_returnsSessionIdAndPopulatesCaches() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .passwordHash("hashed")
                .firstName("Test")
                .lastName("User")
                .build();
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("Password1!");

        when(cacheManager.getCache("sessions")).thenReturn(sessionsCache);
        when(cacheManager.getCache("mfaCodes")).thenReturn(mfaCodesCache);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "hashed")).thenReturn(true);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        LoginResponse response = loginService.login(request);

        assertThat(response.getSessionId()).isNotNull();
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(sessionsCache).put(eq(response.getSessionId()), eq("user@test.com"));
        verify(mfaCodesCache).put(eq(response.getSessionId()), any(MfaEntry.class));
        verify(auditService).sendEvent(eq("MFA_CODE_SENT"), any(), any());
    }

    @Test
    void login_emailNotFound_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("noexiste@test.com");
        request.setPassword("Password1!");

        when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email o contraseña inválidos");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(auditService).sendEvent(eq("LOGIN_FAILED"), any(), any());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentialsException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .passwordHash("hashed")
                .build();
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("WrongPass");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email o contraseña inválidos");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(auditService).sendEvent(eq("LOGIN_FAILED"), any(), any());
    }
}
