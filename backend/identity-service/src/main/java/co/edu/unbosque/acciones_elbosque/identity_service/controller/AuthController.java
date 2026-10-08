package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.LoginRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.MfaResendRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.MfaVerifyRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.request.RegisterRequest;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.LoginResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.MfaVerifyResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.RegisterResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.service.LoginService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.MfaService;
import co.edu.unbosque.acciones_elbosque.identity_service.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterService registerService;
    private final LoginService loginService;
    private final MfaService mfaService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("available", !registerService.emailExists(email)));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<MfaVerifyResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return ResponseEntity.ok(mfaService.verify(request));
    }

    @PostMapping("/mfa/resend")
    public ResponseEntity<Void> resendMfa(@Valid @RequestBody MfaResendRequest request) {
        mfaService.resend(request);
        return ResponseEntity.ok().build();
    }
}
