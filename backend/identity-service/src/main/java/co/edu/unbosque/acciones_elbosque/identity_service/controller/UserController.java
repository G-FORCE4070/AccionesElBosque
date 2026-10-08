package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.UpdatePersonalRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.UpdatePreferencesRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.UserProfileResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getProfile(auth.getName()));
    }

    @PutMapping("/me/personal")
    public ResponseEntity<Void> updatePersonal(Authentication auth,
                                               @Valid @RequestBody UpdatePersonalRequest request) {
        userService.updatePersonal(auth.getName(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<Void> updatePreferences(Authentication auth,
                                                  @Valid @RequestBody UpdatePreferencesRequest request) {
        userService.updatePreferences(auth.getName(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/alpaca/fund")
    public ResponseEntity<Void> fundAlpacaAccount(Authentication auth) {
        userService.fundAlpacaAccount(auth.getName());
        return ResponseEntity.ok().build();
    }
}
