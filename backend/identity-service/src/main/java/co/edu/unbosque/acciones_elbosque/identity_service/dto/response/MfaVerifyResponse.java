package co.edu.unbosque.acciones_elbosque.identity_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MfaVerifyResponse {

    private String token;
    private UserInfo user;

    public record UserInfo(String id, String firstName, String lastName,
                           String email, String role, String plan) {}
}
