package co.edu.unbosque.acciones_elbosque.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MfaVerifyRequest {

    @NotBlank(message = "El sessionId es obligatorio")
    private String sessionId;

    @NotBlank(message = "El código es obligatorio")
    @Size(min = 6, max = 6, message = "El código debe tener exactamente 6 dígitos")
    private String code;
}
