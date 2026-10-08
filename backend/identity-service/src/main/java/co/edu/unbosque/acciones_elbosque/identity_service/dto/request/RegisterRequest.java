package co.edu.unbosque.acciones_elbosque.identity_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(min = 2, message = "El nombre debe tener al menos 2 caracteres")
    private String firstName;

    @NotBlank
    @Size(min = 2, message = "El apellido debe tener al menos 2 caracteres")
    private String lastName;

    @NotBlank
    @Email(message = "Correo electrónico inválido")
    private String email;

    @NotBlank
    @Size(min = 7, message = "Teléfono inválido")
    private String phone;

    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
        message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, un número y un símbolo"
    )
    private String password;

    @NotBlank
    private String birthDate;

    @NotBlank
    @Pattern(regexp = "^(cc|ce|passport)$", message = "Tipo de documento inválido")
    private String docType;

    @NotBlank
    private String docNumber;

    @NotBlank
    private String nationality;

    @NotBlank
    private String country;

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    @NotBlank
    private String postalCode;

    @NotBlank
    @Pattern(regexp = "^(salary|business|investments|pension|other)$", message = "Fuente de ingresos inválida")
    private String incomeSource;

    @NotBlank
    @Pattern(regexp = "^(<30k|30k-100k|100k-500k|>500k)$", message = "Rango de ingresos inválido")
    private String annualIncome;

    @NotBlank
    @Pattern(regexp = "^(<50k|50k-250k|>250k)$", message = "Patrimonio neto inválido")
    private String netWorth;

    @NotBlank
    @Pattern(regexp = "^(conservative|moderate|aggressive)$", message = "Tolerancia al riesgo inválida")
    private String riskTolerance;

    @NotBlank
    @Pattern(regexp = "^(beginner|experienced)$", message = "Experiencia inválida")
    private String tradingExperience;

    @NotBlank
    @Pattern(regexp = "^(preservation|income|growth|speculation)$", message = "Objetivo de inversión inválido")
    private String investmentGoal;

    @NotNull
    private Boolean isPep;

    @NotNull
    private Boolean isFinraAffiliated;

    @NotNull
    @AssertTrue(message = "Debes aceptar los Términos y Condiciones")
    private Boolean acceptsTerms;

    @NotNull
    @AssertTrue(message = "Debes aceptar la Política de Privacidad")
    private Boolean acceptsPrivacy;

    @NotNull
    private Boolean authorizeDataUse;

    @NotEmpty(message = "Selecciona al menos un mercado")
    private List<@Pattern(regexp = "^(nyse|nasdaq)$", message = "Mercado inválido") String> markets;

    @NotEmpty(message = "Selecciona al menos un sector")
    private List<@Pattern(regexp = "^(tech|health|consumer)$", message = "Sector inválido") String> sectors;

    private Boolean notifEmail;
    private Boolean notifSms;
    private Boolean notifWhatsapp;
    private Boolean monthlyReport;

    @NotBlank
    @Pattern(regexp = "^(basic|premium)$", message = "Plan inválido")
    private String plan;

    @Pattern(regexp = "^(monthly|annual)$", message = "Ciclo de facturación inválido")
    private String billingCycle;
}
