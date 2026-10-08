package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.LoginRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.LoginResponse;

public interface LoginService {
    LoginResponse login(LoginRequest request);
}
