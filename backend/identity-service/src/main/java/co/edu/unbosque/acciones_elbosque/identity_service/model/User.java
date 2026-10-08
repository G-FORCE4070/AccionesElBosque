package co.edu.unbosque.acciones_elbosque.identity_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "doc_type", nullable = false, length = 10)
    private String docType;

    @Column(name = "doc_number", nullable = false, length = 50)
    private String docNumber;

    @Column(nullable = false, length = 100)
    private String nationality;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "income_source", nullable = false, length = 50)
    private String incomeSource;

    @Column(name = "annual_income", nullable = false, length = 20)
    private String annualIncome;

    @Column(name = "net_worth", nullable = false, length = 20)
    private String netWorth;

    @Column(name = "risk_tolerance", nullable = false, length = 20)
    private String riskTolerance;

    @Column(name = "trading_experience", nullable = false, length = 20)
    private String tradingExperience;

    @Column(name = "investment_goal", nullable = false, length = 20)
    private String investmentGoal;

    @Column(name = "is_pep", nullable = false)
    private boolean pep;

    @Column(name = "is_finra_affiliated", nullable = false)
    private boolean finraAffiliated;

    @Column(name = "accepts_terms", nullable = false)
    private boolean acceptsTerms;

    @Column(name = "accepts_privacy", nullable = false)
    private boolean acceptsPrivacy;

    @Column(name = "authorize_data_use", nullable = false)
    private boolean authorizeDataUse;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "INVESTOR";

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String plan = "BASIC";

    @Column(name = "alpaca_account_id", length = 100)
    private String alpacaAccountId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
