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
    private boolean isActive;
    private LocalDateTime createdAt;
}