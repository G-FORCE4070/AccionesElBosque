package co.edu.unbosque.acciones_elbosque.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MfaResendRequest {

    @NotBlank(message = "El sessionId es obligatorio")
    private String sessionId;
}
