package com.example.cellex.dtos.request.profile;

import com.example.cellex.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data for account creation")
public class CreateUserDataRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Schema(description = "User's full name", example = "Nguyễn Văn An")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User's email address", example = "admin@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
    @Schema(description = "User's password", example = "Password123")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone number must be 10-11 digits")
    @Schema(description = "User's phone number", example = "0987654321")
    private String phoneNumber;

    @NotNull(message = "Role is required")
    @Schema(description = "User's role in the system", example = "ADMIN")
    private Role role;

    @Size(max = 10, message = "Province code cannot exceed 10 characters")
    @Schema(description = "Vietnam province code (2 digits)", example = "01")
    private String provinceCode;

    @Size(max = 10, message = "Commune code cannot exceed 10 characters")
    @Schema(description = "Vietnam commune/ward code (5 digits)", example = "00001")
    private String communeCode;

    @Size(max = 500, message = "Detail address cannot exceed 500 characters")
    @Schema(description = "Detailed street address", example = "123 Đường Lê Lợi, Khu phố 1")
    private String detailAddress;
}
