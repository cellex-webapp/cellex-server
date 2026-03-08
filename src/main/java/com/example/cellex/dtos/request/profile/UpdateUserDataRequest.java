package com.example.cellex.dtos.request.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data for update operations (profile fields only, address managed separately)")
public class UpdateUserDataRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Schema(description = "User's full name", example = "Nguyễn Văn An")
    private String fullName;

    @Pattern(
        regexp = "^(\\+84|84|0)([3|5|7|8|9])([0-9]{8})$",
        message = "Số điện thoại không đúng định dạng Việt Nam"
    )
    @Schema(description = "Số điện thoại theo định dạng Việt Nam", example = "0987654321")
    private String phoneNumber;
}
