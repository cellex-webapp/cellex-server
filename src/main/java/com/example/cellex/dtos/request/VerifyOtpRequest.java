package com.example.cellex.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @Schema(example = "test@example.com")
    private String email;

    @Schema(example = "123456")
    private String otp;
}