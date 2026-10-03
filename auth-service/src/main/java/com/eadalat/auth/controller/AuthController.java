package com.eadalat.auth.controller;

import com.eadalat.auth.dto.AuthResponse;
import com.eadalat.auth.dto.LoginRequest;
import com.eadalat.auth.dto.RegisterRequest;
import com.eadalat.auth.dto.UserResponse;
import com.eadalat.auth.exception.UnauthorizedException;
import com.eadalat.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new account. PETITIONER and LAWYER are open for self-registration;
     * ADMIN and JUDGE accounts require the requester to hold ROLE_ADMIN
     * (unauthenticated requests therefore always get 403 for those roles).
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req,
                                                 Authentication authentication) {
        boolean requesterIsAdmin = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        UserResponse created = authService.register(req, requesterIsAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /**
     * Current user, resolved from the security context populated by JwtAuthFilter.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("Login required");
        }
        Long userId;
        try {
            userId = Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Login required");
        }
        return ResponseEntity.ok(authService.getById(userId));
    }
}
