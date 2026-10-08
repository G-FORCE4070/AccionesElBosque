package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.RegisterRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.RegisterResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.EmailAlreadyRegisteredException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.model.UserPreference;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserPreferenceRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AlpacaBrokerService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.RegisterService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.StripeCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterServiceImpl implements RegisterService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final AuditService auditService;
    private final AlpacaBrokerService alpacaBrokerService;
    private final StripeCheckoutService stripeCheckoutService;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .birthDate(LocalDate.parse(request.getBirthDate()))
                .docType(request.getDocType())
                .docNumber(request.getDocNumber())
                .nationality(request.getNationality())
                .country(request.getCountry())
                .address(request.getAddress())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .incomeSource(request.getIncomeSource())
                .annualIncome(request.getAnnualIncome())
                .netWorth(request.getNetWorth())
                .riskTolerance(request.getRiskTolerance())
                .tradingExperience(request.getTradingExperience())
                .investmentGoal(request.getInvestmentGoal())
                .pep(Boolean.TRUE.equals(request.getIsPep()))
                .finraAffiliated(Boolean.TRUE.equals(request.getIsFinraAffiliated()))
                .acceptsTerms(Boolean.TRUE.equals(request.getAcceptsTerms()))
                .acceptsPrivacy(Boolean.TRUE.equals(request.getAcceptsPrivacy()))
                .authorizeDataUse(Boolean.TRUE.equals(request.getAuthorizeDataUse()))
                .build();

        User savedUser = userRepository.save(user);

        UserPreference prefs = UserPreference.builder()
                .user(savedUser)
                .notifEmail(!Boolean.FALSE.equals(request.getNotifEmail()))
                .notifSms(Boolean.TRUE.equals(request.getNotifSms()))
                .notifWhatsapp(Boolean.TRUE.equals(request.getNotifWhatsapp()))
                .monthlyReport(Boolean.TRUE.equals(request.getMonthlyReport()))
                .markets(String.join(",", request.getMarkets()))
                .sectors(String.join(",", request.getSectors()))
                .build();
        preferenceRepository.save(prefs);

        String alpacaAccountId = alpacaBrokerService.createAccount(savedUser);

        try {
            alpacaBrokerService.fundAccount(alpacaAccountId);
        } catch (Exception ex) {
            // Alpaca accounts are not immediately active after creation (SUBMITTED → ACTIVE).
            // Funding is deferred and will be retried once the account clears KYC.
            log.warn("Account funding deferred for alpacaId={}: {}", alpacaAccountId, ex.getMessage());
        }

        savedUser.setAlpacaAccountId(alpacaAccountId);
        userRepository.save(savedUser);

        String checkoutUrl = null;
        if ("premium".equals(request.getPlan())) {
            checkoutUrl = stripeCheckoutService.createCheckoutSession(
                    savedUser.getEmail(),
                    request.getBillingCycle()
            );
        }

        try {
            sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName());
        } catch (Exception ex) {
            log.error("Failed to send welcome email to {}: {}", savedUser.getEmail(), ex.getMessage());
        }

        String userId = savedUser.getId().toString();
        auditService.sendEvent("USER_REGISTERED", userId,
                Map.of("email", savedUser.getEmail(), "plan", "BASIC"));
        auditService.sendEvent("ALPACA_ACCOUNT_CREATED", userId,
                Map.of("alpacaAccountId", alpacaAccountId));

        return new RegisterResponse("Registro exitoso. Revisa tu correo de bienvenida.", checkoutUrl);
    }

    private void sendWelcomeEmail(String to, String firstName) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject("Bienvenido a Acciones El Bosque");
            helper.setText(buildWelcomeHtml(firstName), true);
            mailSender.send(mime);
        } catch (jakarta.mail.MessagingException e) {
            throw new MailParseException("Failed to build welcome email", e);
        }
    }

    private String buildWelcomeHtml(String firstName) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><title>Bienvenido a Acciones El Bosque</title></head>
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
                      <td bgcolor="#13141a" style="background:#13141a;padding:36px 32px;">
                        <p style="margin:0 0 6px;font-size:13px;color:#6b7080;text-transform:uppercase;letter-spacing:1px;">Bienvenido</p>
                        <h1 style="margin:0 0 20px;font-size:22px;color:#e8eaf0;font-weight:700;">Hola, %s!</h1>
                        <p style="margin:0 0 24px;font-size:14px;color:#9ca3b0;line-height:1.7;">
                          Tu cuenta en <strong style="color:#e8eaf0;">Acciones El Bosque</strong> ha sido creada exitosamente.
                          Ya puedes iniciar sesion y comenzar a operar con un balance inicial de
                          <strong style="color:#c9a84c;">$50,000 USD</strong> en papel.
                        </p>
                        <table width="100%%" cellpadding="0" cellspacing="0" bgcolor="#0e0f16"
                               style="background:#0e0f16;border:1px solid #2a2b35;border-radius:10px;margin-bottom:24px;">
                          <tr>
                            <td bgcolor="#0e0f16" style="background:#0e0f16;padding:12px 16px;font-size:13px;color:#6b7080;border-bottom:1px solid #2a2b35;">Balance inicial</td>
                            <td bgcolor="#0e0f16" style="background:#0e0f16;padding:12px 16px;font-size:13px;font-weight:700;color:#c9a84c;text-align:right;border-bottom:1px solid #2a2b35;">$50,000.00 USD</td>
                          </tr>
                          <tr>
                            <td bgcolor="#0e0f16" style="background:#0e0f16;padding:12px 16px;font-size:13px;color:#6b7080;border-bottom:1px solid #2a2b35;">Mercados disponibles</td>
                            <td bgcolor="#0e0f16" style="background:#0e0f16;padding:12px 16px;font-size:13px;font-weight:600;color:#e8eaf0;text-align:right;border-bottom:1px solid #2a2b35;">NYSE - NASDAQ</td>
                          </tr>
                          <tr>
                            <td bgcolor="#0e0f16" style="background:#0e0f16;padding:12px 16px;font-size:13px;color:#6b7080;">Plataforma</td>
                            <td bgcolor="#0e0f16" style="background:#0e0f16;padding:12px 16px;font-size:13px;font-weight:600;color:#e8eaf0;text-align:right;">Alpaca Paper Trading</td>
                          </tr>
                        </table>
                        <p style="margin:0;font-size:12px;color:#6b7080;line-height:1.6;">
                          Este es un proyecto academico de la Universidad El Bosque.
                          Las operaciones son simuladas y no involucran dinero real.
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
            """.formatted(firstName);
    }
}
