package co.edu.unbosque.acciones_elbosque.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ConfirmPaymentRequest {

    @NotBlank(message = "El sessionId es requerido")
    private String sessionId;
}
