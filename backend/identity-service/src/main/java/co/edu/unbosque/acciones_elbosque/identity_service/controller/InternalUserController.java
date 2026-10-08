package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.UserPreferencesInternalResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}/preferences")
    public ResponseEntity<UserPreferencesInternalResponse> getPreferences(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getPreferencesInternal(userId));
    }
}
