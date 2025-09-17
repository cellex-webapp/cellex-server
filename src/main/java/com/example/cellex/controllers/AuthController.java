package com.example.cellex.controllers;

import com.example.cellex.dtos.AuthResponse;
import com.example.cellex.dtos.LoginRequest;
import com.example.cellex.dtos.RefreshTokenRequest;
import com.example.cellex.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User Login", description = "Authenticate user with email and password to get tokens.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refresh Token", description = "Get a new access token using a refresh token.")
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    // Endpoint logout sẽ được xử lý phía client bằng cách xóa token.
    // Nếu muốn xử lý phía server, bạn cần một cơ chế blacklist token.
    @Operation(summary = "User Logout", description = "Client should discard the tokens to logout.")
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Phía client sẽ chịu trách nhiệm xóa token
        return ResponseEntity.ok("Logout successful. Please remove your token on the client side.");
    }
}