package com.example.cellex.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("fcmToken")
    private String fcmToken;

    @NotBlank(message = "Device type is required")
    @JsonProperty("deviceType")
    private String deviceType;  // WEB, ANDROID, IOS

    @JsonProperty("deviceName")
    private String deviceName;
}
