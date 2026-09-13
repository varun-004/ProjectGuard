
package com.projectguard.controller;

import com.projectguard.dto.auth.AuthResponse;
import com.projectguard.dto.auth.LoginRequest;
import com.projectguard.dto.auth.RegisterRequest;
import com.projectguard.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                authService.register(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {

        return ResponseEntity.ok(
                "Logout successful. Remove the JWT token from the client."
        );
    }
    @GetMapping("/google-success")
    public ResponseEntity<String> googleSuccess(
            Authentication authentication) {

        return ResponseEntity.ok(
                "Google authentication successful for: "
                        + authentication.getName()
        );
    }

}