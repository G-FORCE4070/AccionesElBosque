package co.edu.unbosque.acciones_elbosque.notification_service.service.impl;

import co.edu.unbosque.acciones_elbosque.notification_service.client.AuditClient;
import co.edu.unbosque.acciones_elbosque.notification_service.client.IdentityClient;
import co.edu.unbosque.acciones_elbosque.notification_service.dto.AuditEventRequest;
import co.edu.unbosque.acciones_elbosque.notification_service.dto.NotificationRequest;
import co.edu.unbosque.acciones_elbosque.notification_service.dto.UserPreferencesResponse;
import co.edu.unbosque.acciones_elbosque.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final IdentityClient identityClient;
    private final AuditClient auditClient;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void processNotification(NotificationRequest request) {
        UserPreferencesResponse prefs = fetchPreferences(request.getUserId());

        if (!prefs.isNotifEmail() || !prefs.isOrderConfirm()) {
            log.info("Notification skipped for userId={} (preferences disabled)", request.getUserId());
            return;
        }

        String userEmail = extractString(request.getData(), "userEmail");
        if (userEmail == null || userEmail.isBlank()) {
            log.warn("No email address available for userId={}, skipping notification", request.getUserId());
            return;
        }

        try {
            sendEmail(userEmail, request.getData());
            auditClient.sendEvent(buildAuditEvent("NOTIFICATION_SENT", request.getUserId(), request.getData()));
        } catch (MailException e) {
            log.error("Failed to send notification email to {}: {}", userEmail, e.getMessage());
            auditClient.sendEvent(buildAuditEvent("NOTIFICATION_FAILED", request.getUserId(), request.getData()));
        }
    }

    private UserPreferencesResponse fetchPreferences(String userId) {
        try {
            return identityClient.getPreferences(userId);
        } catch (RestClientException e) {
            log.warn("Could not fetch preferences for userId={}, using defaults: {}", userId, e.getMessage());
            auditClient.sendEvent(buildAuditEvent("NOTIFICATION_SENT_DEFAULT_PREFS", userId, Map.of()));
            UserPreferencesResponse defaults = new UserPreferencesResponse();
            defaults.setOrderConfirm(true);
            defaults.setNotifEmail(true);
            return defaults;
        }
    }

    private void sendEmail(String to, Map<String, Object> data) throws MailException {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            String from = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@aeb.com";
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Orden ejecutada — Acciones El Bosque");
            helper.setText(buildEmailBody(data), true);
            mailSender.send(mime);
        } catch (MailException e) {
            throw e;
        } catch (jakarta.mail.MessagingException e) {
            throw new MailParseException("Failed to build MIME message", e);
        }
    }

    private String buildEmailBody(Map<String, Object> data) {
        String symbol = extractString(data, "symbol");
        String rawSide = extractString(data, "side");
        String side = "buy".equalsIgnoreCase(rawSide) ? "Compra" : "Venta";
        Object qty = data.get("quantity");
        String executedPrice = formatDecimal(data.get("executedPrice"));
        String commission   = formatDecimal(data.get("commission"));
        String newBalance   = formatDecimal(data.get("newBalance"));

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><title>Orden ejecutada - Acciones El Bosque</title></head>
            <body style="margin:0;padding:0;background:#0a0b0d;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" bgcolor="#0a0b0d" style="background:#0a0b0d;padding:40px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0" bgcolor="#13141a"
                         style="background:#13141a;border:1px solid #2a2b35;border-radius:16px;">
                    <tr>
                      <td bgcolor="#1a1b24" style="background:#1a1b24;padding:28px 32px;border-bottom:2px solid #2a2b35;">
                        <span style="font-size:18px;font-weight:700;color:#c9a84c;letter-spacing:0.5px;">Acciones El Bosque</span>
                        <span style="display:block;font-size:12px;color:#6b7080;margin-top:2px;">Universidad El Bosque</span>
                      </td>
                    </tr>
                    <tr>
                      <td bgcolor="#13141a" style="background:#13141a;padding:32px;">
                        <p style="margin:0 0 6px;font-size:13px;color:#6b7080;text-transform:uppercase;letter-spacing:1px;">Confirmacion de orden</p>
                        <h1 style="margin:0 0 24px;font-size:22px;color:#e8eaf0;font-weight:700;">Orden ejecutada exitosamente</h1>
                        <table width="100%%" cellpadding="0" cellspacing="0" bgcolor="#0e0f16"
                               style="background:#0e0f16;border:1px solid #2a2b35;border-radius:10px;margin-bottom:24px;">
                          %s%s%s%s%s%s
                        </table>
                        <p style="margin:0;font-size:12px;color:#6b7080;line-height:1.6;">
                          Esta notificacion fue generada automaticamente por Acciones El Bosque.<br>
                          Puedes gestionar tus preferencias en la plataforma &rsaquo; Gestion de Perfil.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td bgcolor="#13141a" style="background:#13141a;padding:20px 32px;border-top:1px solid #2a2b35;">
                        <span style="font-size:11px;color:#444754;">&copy; 2026 Universidad El Bosque &mdash; Proyecto academico</span>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(
                row("Accion (simbolo)", symbol),
                row("Tipo de operacion", side),
                row("Cantidad", String.valueOf(qty)),
                row("Precio ejecutado", "$" + executedPrice),
                row("Comision", "$" + commission),
                row("Nuevo saldo disponible", "$" + newBalance)
            );
    }

    private String row(String label, String value) {
        return "<tr>" +
               "<td bgcolor=\"#0e0f16\" style=\"background:#0e0f16;padding:12px 16px;font-size:13px;color:#6b7080;border-bottom:1px solid #2a2b35;\">" + escapeHtml(label) + "</td>" +
               "<td bgcolor=\"#0e0f16\" style=\"background:#0e0f16;padding:12px 16px;font-size:13px;font-weight:600;color:#e8eaf0;text-align:right;border-bottom:1px solid #2a2b35;\">" + escapeHtml(value) + "</td>" +
               "</tr>";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    private String extractString(Map<String, Object> data, String key) {
        Object val = data != null ? data.get(key) : null;
        return val instanceof String s ? s : null;
    }

    private String formatDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd.toPlainString();
        return value != null ? value.toString() : "—";
    }

    private AuditEventRequest buildAuditEvent(String eventType, String userId, Map<String, Object> data) {
        return AuditEventRequest.builder()
                .timestamp(Instant.now())
                .service("notification-service")
                .eventType(eventType)
                .userId(userId)
                .correlationId(userId)
                .details(data)
                .build();
    }
}
