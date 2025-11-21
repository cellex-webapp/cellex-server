package com.example.cellex.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenRequest {
    
    @NotBlank(message = "FCM token is required")
    private String fcmToken;
    
    @NotBlank(message = "Device type is required")
    private String deviceType;  // WEB, ANDROID, IOS
    
    private String deviceName;
}
