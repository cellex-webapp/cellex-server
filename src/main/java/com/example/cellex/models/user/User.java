package com.example.cellex.models.user;

import com.example.cellex.enums.Role;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String id;

    @Field("full_name")
    private String fullName;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password")
    private String password;

    @Field("phone_number")
    private String phoneNumber;

    @Field("avatar_url")
    private String avatarUrl;

    @Field("role")
    private Role role;

    @Field("address")
    private Address address;

    @Field("customer_segment_id")
    private String customerSegmentId;

    @Field("total_spend")
    @Builder.Default
    private Double totalSpend = 0.0; // Tổng chi tiêu của user

    @Field("segment_history")
    private List<SegmentHistory> segmentHistory; // Lịch sử thay đổi phân khúc

    @Field("is_active")
    private boolean isActive;

    @Field("is_banned")
    @Builder.Default
    private boolean isBanned = false;

    @Field("ban_reason")
    private String banReason;

    @Field("banned_at")
    private LocalDateTime bannedAt;

    @Field("banned_by")
    private String bannedBy; // ID của admin thực hiện ban

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.isBanned; // Cập nhật để kiểm tra trạng thái bị cấm
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        @Field("province_code")
        private String provinceCode;

        @Field("province_name")
        private String provinceName;

        @Field("commune_code")
        private String communeCode;

        @Field("commune_name")
        private String communeName;

        @Field("detail_address")
        private String detailAddress;

        @Field("full_address")
        private String fullAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentHistory {
        @Field("segment_id")
        private String segmentId;

        @Field("segment_name")
        private String segmentName;

        @Field("from")
        private LocalDateTime from;

        @Field("to")
        private LocalDateTime to;

        @Field("note")
        private String note; // Ghi chú: "Upgraded", "Downgraded", "Initial"
    }
}
