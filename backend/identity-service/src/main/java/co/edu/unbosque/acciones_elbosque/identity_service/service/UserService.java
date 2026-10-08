package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.UpdatePersonalRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.UpdatePreferencesRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.UserPreferencesInternalResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.UserProfileResponse;

import java.util.UUID;

public interface UserService {

    UserProfileResponse getProfile(String userId);

    void updatePersonal(String userId, UpdatePersonalRequest request);

    void updatePreferences(String userId, UpdatePreferencesRequest request);

    UserPreferencesInternalResponse getPreferencesInternal(UUID userId);

    void fundAlpacaAccount(String userId);
}
