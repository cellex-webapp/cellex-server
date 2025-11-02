package com.example.cellex.controllers;

import com.example.cellex.dtos.request.auth.LoginRequest;
import com.example.cellex.dtos.request.auth.RefreshTokenRequest;
import com.example.cellex.dtos.request.auth.SendOtpRequest;
import com.example.cellex.dtos.request.auth.VerifyOtpRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.auth.AuthResponse;
import com.example.cellex.dtos.response.user.UserResponse;
import com.example.cellex.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "01. Authentication", description = "Endpoints for user authentication and registration")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User Login", description = "Authenticate user with email and password to get tokens.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated (Wrong password)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(request))
                .message("Login successful.")
                .build();
    }

    @Operation(summary = "Refresh Token", description = "Get a new access token using a refresh token.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid refresh token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User associated with token not found")
    })
    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.refreshToken(request))
                .build();
    }

    @Operation(summary = "User Logout", description = "Client should discard the tokens to logout.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful")
    })
    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.<String>builder()
                .message("Logout successful. Please remove your token on the client side.")
                .build();
    }

    @PostMapping("/send-signup-code")
    @Operation(summary = "Send Sign-up OTP", description = "Validates user info, and sends a 6-digit OTP to the user's email.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Passwords do not match or email already exists.")
    })
    public ApiResponse<String> sendSignupCode(@Valid @RequestBody SendOtpRequest request) {
        authService.sendSignupCode(request);
        return ApiResponse.<String>builder()
                .message("An OTP has been sent to your email.")
                .build();
    }

    @PostMapping("/verify-signup-code")
    @Operation(summary = "Verify OTP & Create Account", description = "Verifies the OTP, creates the user account, and returns auth tokens.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account created and user logged in successfully."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid, expired, or already used OTP.")
    })
    public ApiResponse<UserResponse> verifySignupCode(@Valid @RequestBody VerifyOtpRequest request) {
        UserResponse userResponse = authService.verifySignupCode(request);
        return ApiResponse.<UserResponse>builder()
                .result(userResponse)
                .message("Account created successfully.")
                .build();
    }
}