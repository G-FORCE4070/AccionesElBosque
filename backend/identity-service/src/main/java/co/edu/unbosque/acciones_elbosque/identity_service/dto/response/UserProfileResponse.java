package co.edu.unbosque.acciones_elbosque.identity_service.dto.response;

import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.model.UserPreference;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UserProfileResponse {

    private final String id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final LocalDate birthDate;
    private final String nationality;
    private final String docType;
    private final String docNumber;
    private final String country;
    private final String city;
    private final String address;
    private final String postalCode;
    private final String incomeSource;
    private final String annualIncome;
    private final String netWorth;
    private final String riskTolerance;
    private final String tradingExperience;
    private final String investmentGoal;
    private final String role;
    private final String plan;
    private final PreferencesResponse preferences;
    private final Object broker;

    public UserProfileResponse(User user, UserPreference pref) {
        this.id = user.getId().toString();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.birthDate = user.getBirthDate();
        this.nationality = user.getNationality();
        this.docType = user.getDocType();
        this.docNumber = user.getDocNumber();
        this.country = user.getCountry();
        this.city = user.getCity();
        this.address = user.getAddress();
        this.postalCode = user.getPostalCode();
        this.incomeSource = user.getIncomeSource();
        this.annualIncome = user.getAnnualIncome();
        this.netWorth = user.getNetWorth();
        this.riskTolerance = user.getRiskTolerance();
        this.tradingExperience = user.getTradingExperience();
        this.investmentGoal = user.getInvestmentGoal();
        this.role = user.getRole();
        this.plan = user.getPlan();
        this.preferences = new PreferencesResponse(pref);
        this.broker = null;
    }
}
