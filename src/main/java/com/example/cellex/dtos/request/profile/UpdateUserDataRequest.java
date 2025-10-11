package com.example.cellex.dtos.request.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data for update operations")
public class UpdateUserDataRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Schema(description = "User's full name", example = "Nguyễn Văn An")
    private String fullName;

    @Size(max = 10, message = "Province code cannot exceed 10 characters")
    @Schema(description = "Province code", example = "01")
    private String provinceCode;

    @Size(max = 10, message = "Commune code cannot exceed 10 characters")
    @Schema(description = "Commune code", example = "00001")
    private String communeCode;

    @Size(max = 500, message = "Detail address cannot exceed 500 characters")
    @Schema(description = "Detail address", example = "123 Đường Lê Lợi")
    private String detailAddress;
}
