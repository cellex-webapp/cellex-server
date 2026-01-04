package com.example.cellex.dtos.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
    @Schema(example = "test@example.com")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Schema(example = "John Doe")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone number must be 10-11 digits")
    @Schema(example = "0987654321")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    @Schema(example = "password123")
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Schema(example = "password123")
    private String confirmPassword;
}
