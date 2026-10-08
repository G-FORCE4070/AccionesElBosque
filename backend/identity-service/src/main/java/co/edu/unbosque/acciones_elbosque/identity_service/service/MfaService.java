package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.MfaResendRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.MfaVerifyRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.MfaVerifyResponse;

public interface MfaService {
    MfaVerifyResponse verify(MfaVerifyRequest request);
    void resend(MfaResendRequest request);
}
