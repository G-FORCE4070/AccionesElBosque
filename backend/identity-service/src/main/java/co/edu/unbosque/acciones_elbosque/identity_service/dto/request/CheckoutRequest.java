package co.edu.unbosque.acciones_elbosque.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {

    @NotBlank(message = "billingCycle es obligatorio")
    @Pattern(regexp = "monthly|annual", message = "billingCycle debe ser monthly o annual")
    private String billingCycle;
}
