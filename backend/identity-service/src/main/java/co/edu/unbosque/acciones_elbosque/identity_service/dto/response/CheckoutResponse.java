package co.edu.unbosque.acciones_elbosque.identity_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckoutResponse {
    private final String checkoutUrl;
}
