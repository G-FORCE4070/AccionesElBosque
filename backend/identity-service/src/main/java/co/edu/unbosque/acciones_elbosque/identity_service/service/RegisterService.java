package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.RegisterRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.RegisterResponse;

public interface RegisterService {
    RegisterResponse register(RegisterRequest request);
    boolean emailExists(String email);
}
