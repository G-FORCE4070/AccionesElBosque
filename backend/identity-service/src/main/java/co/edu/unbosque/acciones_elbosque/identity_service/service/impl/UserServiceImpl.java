package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.UpdatePersonalRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.UpdatePreferencesRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.UserPreferencesInternalResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.UserProfileResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.UserNotFoundException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.User;
import co.edu.unbosque.acciones_elbosque.identity_service.model.UserPreference;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserPreferenceRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.UserRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AlpacaBrokerService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.AuditService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final AuditService auditService;
    private final AlpacaBrokerService alpacaBrokerService;

    @Override
    public UserProfileResponse getProfile(String userId) {
        User user = findUser(userId);
        UserPreference pref = preferenceRepository.findById(user.getId())
                .orElseGet(() -> UserPreference.builder().user(user).build());
        return new UserProfileResponse(user, pref);
    }

    @Override
    @Transactional
    public void updatePersonal(String userId, UpdatePersonalRequest request) {
        User user = findUser(userId);

        // Contact info
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getPostalCode() != null) user.setPostalCode(request.getPostalCode());

        // Financial profile
        if (request.getIncomeSource() != null) user.setIncomeSource(request.getIncomeSource());
        if (request.getAnnualIncome() != null) user.setAnnualIncome(request.getAnnualIncome());
        if (request.getNetWorth() != null) user.setNetWorth(request.getNetWorth());
        if (request.getRiskTolerance() != null) user.setRiskTolerance(request.getRiskTolerance());
        if (request.getTradingExperience() != null) user.setTradingExperience(request.getTradingExperience());
        if (request.getInvestmentGoal() != null) user.setInvestmentGoal(request.getInvestmentGoal());

        // KYC fields — only updated if explicitly sent (API compatibility; UI does not expose these)
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getBirthDate() != null) user.setBirthDate(LocalDate.parse(request.getBirthDate()));
        if (request.getNationality() != null) user.setNationality(request.getNationality());

        userRepository.save(user);
        auditService.sendEvent("PROFILE_UPDATED", userId, Map.of("section", "personal"));
    }

    @Override
    @Transactional
    public void updatePreferences(String userId, UpdatePreferencesRequest request) {
        User user = findUser(userId);
        UserPreference pref = preferenceRepository.findById(user.getId())
                .orElseGet(() -> UserPreference.builder().user(user).build());

        if (request.getMarketOpen() != null) pref.setMarketOpen(request.getMarketOpen());
        if (request.getPriceAlerts() != null) pref.setPriceAlerts(request.getPriceAlerts());
        if (request.getDailySummary() != null) pref.setDailySummary(request.getDailySummary());
        if (request.getOrderConfirm() != null) pref.setOrderConfirm(request.getOrderConfirm());
        if (request.getNotifEmail() != null) pref.setNotifEmail(request.getNotifEmail());
        if (request.getNotifSms() != null) pref.setNotifSms(request.getNotifSms());
        if (request.getNotifWhatsapp() != null) pref.setNotifWhatsapp(request.getNotifWhatsapp());
        if (request.getMonthlyReport() != null) pref.setMonthlyReport(request.getMonthlyReport());
        if (request.getViewType() != null) pref.setViewType(request.getViewType());
        if (request.getDefaultOrder() != null) pref.setDefaultOrder(request.getDefaultOrder());
        if (request.getShowLowWeight() != null) pref.setShowLowWeight(request.getShowLowWeight());
        if (request.getDefaultOrderType() != null) pref.setDefaultOrderType(request.getDefaultOrderType());
        if (request.getDailyLimit() != null) pref.setDailyLimit(request.getDailyLimit());
        if (request.getRequireDoubleAuth() != null) pref.setRequireDoubleAuth(request.getRequireDoubleAuth());
        if (request.getMarket() != null) pref.setMarkets(request.getMarket());
        if (request.getSector() != null) pref.setSectors(request.getSector());

        preferenceRepository.save(pref);
        auditService.sendEvent("PROFILE_UPDATED", userId, Map.of("section", "preferences"));
    }

    @Override
    public UserPreferencesInternalResponse getPreferencesInternal(UUID userId) {
        userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        UserPreference pref = preferenceRepository.findById(userId)
                .orElseGet(() -> UserPreference.builder().build());
        return new UserPreferencesInternalResponse(pref.isOrderConfirm(), pref.isNotifEmail());
    }

    @Override
    public void fundAlpacaAccount(String userId) {
        User user = findUser(userId);
        if (user.getAlpacaAccountId() == null) {
            throw new UserNotFoundException();
        }
        alpacaBrokerService.fundAccount(user.getAlpacaAccountId());
        auditService.sendEvent("ALPACA_ACCOUNT_FUNDED", userId,
                Map.of("alpacaAccountId", user.getAlpacaAccountId()));
    }

    private User findUser(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(UserNotFoundException::new);
    }
}
