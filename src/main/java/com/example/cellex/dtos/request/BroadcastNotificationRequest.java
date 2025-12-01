package com.example.cellex.dtos.request;

import com.example.cellex.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastNotificationRequest {
    
    @NotBlank(message = "Title is required")
    @Schema(description = "Notification title", example = "string", required = true)
    private String title;
    
    @NotBlank(message = "Message is required")
    @Schema(description = "Notification message", example = "string", required = true)
    private String message;
    
    @NotNull(message = "Type is required")
    @Schema(description = "Notification type", example = "SYSTEM", required = true)
    private NotificationType type;
    
    @Schema(description = "Additional metadata", example = "string")
    private String metadata;
    
    @Schema(description = "Action URL", example = "string")
    private String actionUrl;
    
    @Schema(description = "Image file for notification", type = "string", format = "binary")
    private MultipartFile imageFile;

    @Schema(description = "Expiration date and time", example = "2025-11-30T05:14:23.151Z")
    // Use String to simplify binding from multipart/form-data (supports Z/offset). Parse later in controller.
    private String expiresAt;
}
