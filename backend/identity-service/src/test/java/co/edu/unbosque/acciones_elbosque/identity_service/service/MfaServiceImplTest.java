package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.MfaResendRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.MfaVerifyRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.MfaVerifyResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.InvalidMfaCodeException;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.MfaResendLimitException;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.MfaSessionNotFoundException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.MfaEntry;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.impl.MfaServiceImpl;
import co.edu.unbosque.acciones_elbosque.identity_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;
import static org.mockito.Mockito.mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceImplTest {

    @Mock private CacheManager cacheManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuditService auditService;
    @Mock private JavaMailSender mailSender;
    @Mock private Cache mfaCache;
    @Mock private Cache sessionCache;

    @InjectMocks private MfaServiceImpl mfaService;

    private static final String SESSION_ID = "session-123";
    private static final String EMAIL = "user@test.com";
    private static final String VALID_CODE = "123456";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mfaService, "mailFrom", "noreply@test.com");
        when(cacheManager.getCache("mfaCodes")).thenReturn(mfaCache);
        when(cacheManager.getCache("sessions")).thenReturn(sessionCache);
    }

    @Test
    void verify_success_returnsTokenAndEvictsCaches() {
        MfaEntry entry = new MfaEntry(VALID_CODE, 0, 0);
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .firstName("Test")
                .lastName("User")
                .role("INVESTOR")
                .plan("BASIC")
                .build();

        when(mfaCache.get(SESSION_ID, MfaEntry.class)).thenReturn(entry);
        when(sessionCache.get(SESSION_ID, String.class)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setSessionId(SESSION_ID);
        request.setCode(VALID_CODE);

        MfaVerifyResponse response = mfaService.verify(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().email()).isEqualTo(EMAIL);
        verify(mfaCache).evict(SESSION_ID);
        verify(sessionCache).evict(SESSION_ID);
        verify(auditService).sendEvent(eq("LOGIN_SUCCESS"), any(), any());
    }

    @Test
    void verify_wrongCode_firstAttempt_throwsInvalidMfaCodeException() {
        MfaEntry entry = new MfaEntry(VALID_CODE, 0, 0);
        when(mfaCache.get(SESSION_ID, MfaEntry.class)).thenReturn(entry);

        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setSessionId(SESSION_ID);
        request.setCode("000000");

        assertThatThrownBy(() -> mfaService.verify(request))
                .isInstanceOf(InvalidMfaCodeException.class);

        assertThat(entry.getAttempts()).isEqualTo(1);
        verify(mfaCache).put(eq(SESSION_ID), eq(entry));
        verify(mfaCache, never()).evict(any());
    }

    @Test
    void verify_wrongCode_thirdAttempt_evictsCacheAndThrowsInvalidMfaCode() {
        // Per spec CA-05: 3rd failed attempt returns 400 (not 404) and evicts cache.
        // The 404 only happens on the 4th attempt when cache is already empty.
        MfaEntry entry = new MfaEntry(VALID_CODE, 2, 0);
        when(mfaCache.get(SESSION_ID, MfaEntry.class)).thenReturn(entry);

        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setSessionId(SESSION_ID);
        request.setCode("000000");

        assertThatThrownBy(() -> mfaService.verify(request))
                .isInstanceOf(InvalidMfaCodeException.class);

        verify(mfaCache).evict(SESSION_ID);
        verify(sessionCache).evict(SESSION_ID);
    }

    @Test
    void verify_expiredSession_throwsMfaSessionNotFoundException() {
        when(mfaCache.get(SESSION_ID, MfaEntry.class)).thenReturn(null);

        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setSessionId(SESSION_ID);
        request.setCode(VALID_CODE);

        assertThatThrownBy(() -> mfaService.verify(request))
                .isInstanceOf(MfaSessionNotFoundException.class)
                .hasMessage("El código ha expirado");
    }

    @Test
    void resend_success_updatesCodeAndSendsEmail() {
        MfaEntry entry = new MfaEntry(VALID_CODE, 1, 0);
        when(mfaCache.get(SESSION_ID, MfaEntry.class)).thenReturn(entry);
        when(sessionCache.get(SESSION_ID, String.class)).thenReturn(EMAIL);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        doNothing().when(mailSender).send(any(MimeMessage.class));

        MfaResendRequest request = new MfaResendRequest();
        request.setSessionId(SESSION_ID);

        mfaService.resend(request);

        assertThat(entry.getResends()).isEqualTo(1);
        assertThat(entry.getAttempts()).isEqualTo(0);
        verify(mfaCache).put(eq(SESSION_ID), eq(entry));
        verify(mailSender).send(any(MimeMessage.class));
        verify(auditService).sendEvent(eq("MFA_CODE_SENT"), any(), any());
    }

    @Test
    void resend_limitReached_throwsMfaResendLimitException() {
        MfaEntry entry = new MfaEntry(VALID_CODE, 0, 3);
        when(mfaCache.get(SESSION_ID, MfaEntry.class)).thenReturn(entry);

        MfaResendRequest request = new MfaResendRequest();
        request.setSessionId(SESSION_ID);

        assertThatThrownBy(() -> mfaService.resend(request))
                .isInstanceOf(MfaResendLimitException.class)
                .hasMessage("Máximo de reenvíos alcanzado");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
