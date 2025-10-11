package com.example.cellex.dtos.request.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new user account with data and avatar parts")
public class CreateUserRequest {

    @NotNull(message = "User data is required")
    @Valid
    @Schema(description = "User account data", implementation = CreateUserDataRequest.class)
    private CreateUserDataRequest data;

    @Schema(description = "Avatar image file (JPEG, PNG, WebP - max 5MB)", type = "string", format = "binary")
    private MultipartFile avatar;
}
