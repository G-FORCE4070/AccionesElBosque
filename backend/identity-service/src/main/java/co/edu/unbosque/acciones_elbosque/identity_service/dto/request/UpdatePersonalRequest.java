package co.edu.unbosque.acciones_elbosque.identity_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePersonalRequest {

    // Contact info — updateable
    private String address;
    private String city;
    private String country;

    @Size(min = 4, max = 10, message = "El código postal debe tener entre 4 y 10 caracteres")
    private String postalCode;

    // Financial profile — updateable
    private String incomeSource;
    private String annualIncome;
    private String netWorth;
    private String riskTolerance;
    private String tradingExperience;
    private String investmentGoal;

    // KYC identity fields — kept for API compatibility but not exposed in UI
    @Size(min = 2, message = "El nombre debe tener al menos 2 caracteres")
    private String firstName;

    @Size(min = 2, message = "El apellido debe tener al menos 2 caracteres")
    private String lastName;

    @Size(min = 7, message = "El teléfono debe tener al menos 7 caracteres")
    private String phone;

    private String birthDate;

    private String nationality;
}
