package com.example.cellex.dtos.request.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update user profile request")
public class UpdateUserRequest {

    @Schema(description = "User's full name", example = "Nguyen Van A")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Pattern(
        regexp = "^(\\+84|84|0)([3|5|7|8|9])([0-9]{8})$",
        message = "Số điện thoại không đúng định dạng Việt Nam"
    )
    @Schema(description = "Số điện thoại theo định dạng Việt Nam", example = "0987654321")
    private String phoneNumber;

    @Schema(description = "User's avatar image file", type = "string", format = "binary")
    private MultipartFile avatar;

    @Schema(description = "Province code", example = "01")
    @Size(max = 10, message = "Province code cannot exceed 10 characters")
    private String provinceCode;

    @Schema(description = "Commune code", example = "00001")
    @Size(max = 10, message = "Commune code cannot exceed 10 characters")
    private String communeCode;

    @Schema(description = "Detail address", example = "123 Main Street")
    @Size(max = 500, message = "Detail address cannot exceed 500 characters")
    private String detailAddress;
}
