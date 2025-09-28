package com.example.cellex.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SendOtpRequest {
    @Schema(example = "test@example.com")
    private String email;

    @Schema(example = "John Doe")
    private String fullName;

    @Schema(example = "0987654321")
    private String phoneNumber;

    @Schema(example = "password123")
    private String password;

    @Schema(example = "password123")
    private String confirmPassword;
}