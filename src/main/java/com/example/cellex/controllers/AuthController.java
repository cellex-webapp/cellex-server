package com.example.cellex.controllers;

import com.example.cellex.dtos.request.LoginRequest; // Cập nhật
import com.example.cellex.dtos.request.RefreshTokenRequest; // Cập nhật
import com.example.cellex.dtos.response.ApiResponse; // Cập nhật
import com.example.cellex.dtos.response.AuthResponse; // Cập nhật
import com.example.cellex.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(request))
                .message("Login successful.")
                .build();
    }

    @Operation(summary = "Refresh Token", description = "Get a new access token using a refresh token.")
    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.refreshToken(request))
                .build();
    }

    @Operation(summary = "User Logout", description = "Client should discard the tokens to logout.")
    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.<String>builder()
                .message("Logout successful. Please remove your token on the client side.")
                .build();
    }
}