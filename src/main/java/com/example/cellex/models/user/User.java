package com.example.cellex.models.user;

import com.example.cellex.enums.Role;
import com.example.cellex.models.jpa.RoleEntity;
import com.example.cellex.models.jpa.UserAddressEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JPA Entity for the 'users' table in PostgreSQL (Supabase).
 * Migrated from MongoDB @Document to JPA @Entity.
 * Maintains backward-compatible API (getRole(), getAddress(), getId() as String).
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /**
     * Backward-compatible single role field.
     * Stored as VARCHAR in PostgreSQL. Maps to the old Role enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * Cross-database reference to customer segment.
     * Stored as PostgreSQL UUID in the users table.
     */
    @JsonIgnore
    @Column(name = "customer_segment_id")
    private UUID customerSegmentId;

    @JsonProperty("customerSegmentId")
    public String getCustomerSegmentId() {
        return customerSegmentId != null ? customerSegmentId.toString() : null;
    }

    public void setCustomerSegmentId(String customerSegmentId) {
        this.customerSegmentId = (customerSegmentId != null && !customerSegmentId.isEmpty())
                ? UUID.fromString(customerSegmentId)
                : null;
    }

    @Column(name = "total_spend", precision = 15, scale = 2)
    @Builder.Default
    @JsonIgnore
    private BigDecimal totalSpendDecimal = BigDecimal.ZERO;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "is_banned")
    @Builder.Default
    private boolean isBanned = false;

    @Column(name = "ban_reason", columnDefinition = "TEXT")
    private String banReason;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "banned_by")
    @JsonIgnore
    private UUID bannedByUuid;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== RBAC Relationships (new) ====================

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    @JsonIgnore
    private Set<RoleEntity> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<UserAddressEntity> addresses = new ArrayList<>();

    // ==================== Backward-compatible ID accessors ====================

    /**
     * Returns the user ID as String (backward compatible with old MongoDB String IDs).
     * All existing services/controllers use getId() → String.
     */
    @JsonProperty("id")
    public String getId() {
        return uuid != null ? uuid.toString() : null;
    }

    /**
     * Sets the user ID from a String (backward compatible).
     * Accepts both UUID format strings and null.
     */
    public void setId(String id) {
        this.uuid = (id != null && !id.isEmpty()) ? UUID.fromString(id) : null;
    }

    // ==================== Backward-compatible totalSpend accessors ====================

    /**
     * Returns total spend as Double (backward compatible).
     */
    @JsonProperty("totalSpend")
    public Double getTotalSpend() {
        return totalSpendDecimal != null ? totalSpendDecimal.doubleValue() : 0.0;
    }

    /**
     * Sets total spend from Double (backward compatible).
     */
    public void setTotalSpend(Double totalSpend) {
        this.totalSpendDecimal = totalSpend != null ? BigDecimal.valueOf(totalSpend) : BigDecimal.ZERO;
    }

    // ==================== Backward-compatible bannedBy accessors ====================

    @JsonProperty("bannedBy")
    public String getBannedBy() {
        return bannedByUuid != null ? bannedByUuid.toString() : null;
    }

    public void setBannedBy(String bannedBy) {
        this.bannedByUuid = (bannedBy != null && !bannedBy.isEmpty()) ? UUID.fromString(bannedBy) : null;
    }

    // ==================== Backward-compatible Address accessors ====================

    /**
     * Returns the default address as the old Address inner class (backward compatible).
     */
    @Transient
    public Address getAddress() {
        UserAddressEntity defaultAddr = addresses.stream()
                .filter(UserAddressEntity::isDefault)
                .findFirst()
                .orElse(addresses.isEmpty() ? null : addresses.get(0));
        if (defaultAddr == null) return null;
        return Address.builder()
                .provinceCode(defaultAddr.getProvinceCode())
                .provinceName(defaultAddr.getProvinceName())
                .communeCode(defaultAddr.getCommuneCode())
                .communeName(defaultAddr.getCommuneName())
                .detailAddress(defaultAddr.getDetailAddress())
                .fullAddress(defaultAddr.getFullAddress())
                .build();
    }

    /**
     * Sets the default address from the old Address inner class (backward compatible).
     * Creates or updates the default UserAddressEntity.
     */
    @Transient
    public void setAddress(Address address) {
        if (address == null) return;
        UserAddressEntity defaultAddr = addresses.stream()
                .filter(UserAddressEntity::isDefault)
                .findFirst()
                .orElse(null);
        if (defaultAddr == null) {
            defaultAddr = new UserAddressEntity();
            defaultAddr.setUser(this);
            defaultAddr.setDefault(true);
            addresses.add(defaultAddr);
        }
        defaultAddr.setProvinceCode(address.getProvinceCode());
        defaultAddr.setProvinceName(address.getProvinceName());
        defaultAddr.setCommuneCode(address.getCommuneCode());
        defaultAddr.setCommuneName(address.getCommuneName());
        defaultAddr.setDetailAddress(address.getDetailAddress());
        defaultAddr.setFullAddress(address.getFullAddress());
    }

    // ==================== UserDetails Implementation ====================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Primary: use RBAC roles if available
        if (roles != null && !roles.isEmpty()) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            for (RoleEntity r : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getRoleName()));
            }
            return authorities;
        }
        // Fallback: use single role field
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
        return !this.isBanned;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isActive;
    }

    // ==================== Inner classes (backward compatible) ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String provinceCode;
        private String provinceName;
        private String communeCode;
        private String communeName;
        private String detailAddress;
        private String fullAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentHistory {
        private String segmentId;
        private String segmentName;
        private LocalDateTime from;
        private LocalDateTime to;
        private String note;
    }
}
