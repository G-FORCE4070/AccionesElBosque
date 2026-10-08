package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.UpdatePersonalRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.UpdatePreferencesRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.UserProfileResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.UserNotFoundException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.model.UserPreference;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserPreferenceRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository preferenceRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;
    private UserPreference pref;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@test.com")
                .passwordHash("hash")
                .firstName("Ana")
                .lastName("Gómez")
                .phone("+57300000000")
                .birthDate(LocalDate.of(1990, 1, 1))
                .docType("cc")
                .docNumber("12345678")
                .nationality("Colombiana")
                .country("CO")
                .address("Calle 1")
                .city("Bogotá")
                .postalCode("110111")
                .incomeSource("salary")
                .annualIncome("30k-100k")
                .netWorth("50k-250k")
                .riskTolerance("moderate")
                .tradingExperience("beginner")
                .investmentGoal("growth")
                .role("INVESTOR")
                .plan("BASIC")
                .build();

        pref = UserPreference.builder()
                .user(user)
                .markets("nyse")
                .sectors("tech")
                .build();
    }

    @Test
    void getProfile_existingUser_returnsFullProfile() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(pref));

        UserProfileResponse response = userService.getProfile(userId.toString());

        assertThat(response.getFirstName()).isEqualTo("Ana");
        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getPreferences().getMarket()).isEqualTo("nyse");
    }

    @Test
    void getProfile_unknownUser_throwsUserNotFoundException() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(UUID.randomUUID().toString()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updatePersonal_validFields_persistsAndFiresAudit() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UpdatePersonalRequest request = new UpdatePersonalRequest();
        request.setFirstName("María");
        request.setPhone("+57311111111");

        userService.updatePersonal(userId.toString(), request);

        assertThat(user.getFirstName()).isEqualTo("María");
        assertThat(user.getPhone()).isEqualTo("+57311111111");
        assertThat(user.getLastName()).isEqualTo("Gómez");
        verify(auditService).sendEvent(eq("PROFILE_UPDATED"), eq(userId.toString()), any());
    }

    @Test
    void updatePersonal_unknownUser_throwsUserNotFoundException() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePersonal(UUID.randomUUID().toString(), new UpdatePersonalRequest()))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePreferences_validFields_persistsAndFiresAudit() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(pref));
        when(preferenceRepository.save(any())).thenReturn(pref);

        UpdatePreferencesRequest request = new UpdatePreferencesRequest();
        request.setMarketOpen(true);
        request.setMarket("nasdaq");

        userService.updatePreferences(userId.toString(), request);

        assertThat(pref.isMarketOpen()).isTrue();
        assertThat(pref.getMarkets()).isEqualTo("nasdaq");
        assertThat(pref.getSectors()).isEqualTo("tech");
        verify(auditService).sendEvent(eq("PROFILE_UPDATED"), eq(userId.toString()), any());
    }
}
