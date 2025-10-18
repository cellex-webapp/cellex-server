package com.example.cellex.dtos.response;

import com.example.cellex.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private Role role;
    private AddressResponse address;
    private String customerSegmentId;
    private boolean isActive;
    private boolean isBanned;
    private String banReason;
    private LocalDateTime bannedAt;
    private String bannedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private String street;
        private String ward;
        private String province;

        @Builder.Default
        private String country = "Việt Nam";

        private String fullAddress;
        private boolean isDefault;
    }
}