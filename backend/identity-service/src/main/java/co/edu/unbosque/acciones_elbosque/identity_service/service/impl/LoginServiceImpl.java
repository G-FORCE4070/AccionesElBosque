package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.LoginRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.LoginResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.InvalidCredentialsException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.MfaEntry;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginServiceImpl implements LoginService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final CacheManager cacheManager;
    private final AuditService auditService;

    @Value("${app.mail.from}")
    private String mailFrom;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditService.sendEvent("LOGIN_FAILED", request.getEmail(), Map.of("reason", "email_not_found"));
                    return new InvalidCredentialsException("Email o contraseña inválidos");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.sendEvent("LOGIN_FAILED", user.getId().toString(), Map.of("reason", "wrong_password"));
            throw new InvalidCredentialsException("Email o contraseña inválidos");
        }

        String sessionId = UUID.randomUUID().toString();
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        sendMfaEmail(user.getEmail(), code);

        cacheManager.getCache("sessions").put(sessionId, user.getEmail());
        cacheManager.getCache("mfaCodes").put(sessionId, new MfaEntry(code, 0, 0));

        auditService.sendEvent("MFA_CODE_SENT", user.getId().toString(), Map.of("sessionId", sessionId));

        return new LoginResponse(sessionId);
    }

    private void sendMfaEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject("Tu código de verificación — Acciones ElBosque");
        message.setText("Tu código de verificación es: " + code + "\n\nEste código expira en 5 minutos.");
        mailSender.send(message);
    }
}
