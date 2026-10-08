package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.MfaResendRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.MfaVerifyRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.MfaVerifyResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.InvalidMfaCodeException;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.MfaResendLimitException;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.MfaSessionNotFoundException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.MfaEntry;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.MfaService;
import co.edu.unbosque.acciones_elbosque.identity_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private final CacheManager cacheManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_RESENDS = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public MfaVerifyResponse verify(MfaVerifyRequest request) {
        Cache mfaCache = cacheManager.getCache("mfaCodes");
        Cache sessionCache = cacheManager.getCache("sessions");

        MfaEntry entry = mfaCache.get(request.getSessionId(), MfaEntry.class);
        if (entry == null) {
            throw new MfaSessionNotFoundException("El código ha expirado");
        }

        if (!entry.getCode().equals(request.getCode())) {
            entry.setAttempts(entry.getAttempts() + 1);
            auditService.sendEvent("MFA_FAILED", request.getSessionId(),
                    Map.of("attempts", entry.getAttempts()));

            if (entry.getAttempts() >= MAX_ATTEMPTS) {
                // 3rd failed attempt: evict cache but still return 400 per spec CA-05.
                // The next attempt will return 404 because the session no longer exists.
                mfaCache.evict(request.getSessionId());
                sessionCache.evict(request.getSessionId());
            } else {
                mfaCache.put(request.getSessionId(), entry);
            }
            throw new InvalidMfaCodeException("Código inválido");
        }

        String email = sessionCache.get(request.getSessionId(), String.class);
        if (email == null) {
            throw new MfaSessionNotFoundException("El código ha expirado");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MfaSessionNotFoundException("El código ha expirado"));

        String token = jwtUtil.generateToken(user);

        mfaCache.evict(request.getSessionId());
        sessionCache.evict(request.getSessionId());

        auditService.sendEvent("LOGIN_SUCCESS", user.getId().toString(), Map.of());

        MfaVerifyResponse.UserInfo userInfo = new MfaVerifyResponse.UserInfo(
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getPlan()
        );
        return new MfaVerifyResponse(token, userInfo);
    }

    @Override
    public void resend(MfaResendRequest request) {
        Cache mfaCache = cacheManager.getCache("mfaCodes");
        Cache sessionCache = cacheManager.getCache("sessions");

        MfaEntry entry = mfaCache.get(request.getSessionId(), MfaEntry.class);
        if (entry == null) {
            throw new MfaSessionNotFoundException("El código ha expirado");
        }

        if (entry.getResends() >= MAX_RESENDS) {
            throw new MfaResendLimitException("Máximo de reenvíos alcanzado");
        }

        String newCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        entry.setCode(newCode);
        entry.setAttempts(0);
        entry.setResends(entry.getResends() + 1);
        mfaCache.put(request.getSessionId(), entry);

        String email = sessionCache.get(request.getSessionId(), String.class);
        if (email != null) {
            sendMfaEmail(email, newCode);
        }

        auditService.sendEvent("MFA_CODE_SENT", request.getSessionId(),
                Map.of("resend", entry.getResends()));
    }

    private void sendMfaEmail(String to, String code) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject("Código de verificación — Acciones El Bosque");
            helper.setText(buildMfaHtml(code), true);
            mailSender.send(mime);
        } catch (jakarta.mail.MessagingException e) {
            throw new MailParseException("Failed to build MFA email", e);
        }
    }

    private String buildMfaHtml(String code) {
        String[] digits = code.split("");
        StringBuilder boxes = new StringBuilder();
        for (String d : digits) {
            boxes.append("""
                <span style="display:inline-block;width:44px;height:52px;line-height:52px;text-align:center;
                             background:#0e0f16;border:1px solid #2a2b35;border-radius:8px;
                             font-size:26px;font-weight:700;color:#c9a84c;margin:0 4px;">%s</span>
                """.formatted(d));
        }
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#0a0b0d;font-family:'Segoe UI',sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0a0b0d;padding:40px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0"
                         style="background:#13141a;border:1px solid #2a2b35;border-radius:16px;overflow:hidden;">
                    <tr>
                      <td style="background:#1a1b24;padding:28px 32px;border-bottom:1px solid #2a2b35;">
                        <span style="font-size:18px;font-weight:700;color:#c9a84c;letter-spacing:0.5px;">Acciones El Bosque</span>
                        <span style="display:block;font-size:12px;color:#6b7080;margin-top:2px;">Universidad El Bosque</span>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 32px;text-align:center;">
                        <p style="margin:0 0 6px;font-size:13px;color:#6b7080;text-transform:uppercase;letter-spacing:1px;">Verificación de identidad</p>
                        <h1 style="margin:0 0 8px;font-size:22px;color:#e8eaf0;font-weight:700;">Tu código de acceso</h1>
                        <p style="margin:0 0 28px;font-size:14px;color:#9ca3b0;">Ingresa este código en la plataforma para completar el inicio de sesión.</p>
                        <div style="margin:0 0 28px;">%s</div>
                        <p style="margin:0;font-size:13px;color:#6b7080;">
                          Este código <strong style="color:#e8eaf0;">expira en 5 minutos</strong>.<br>
                          Si no solicitaste este acceso, ignora este correo.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:20px 32px;border-top:1px solid #2a2b35;">
                        <span style="font-size:11px;color:#444754;">&copy; 2025 Universidad El Bosque &mdash; Proyecto académico</span>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(boxes.toString());
    }
}
