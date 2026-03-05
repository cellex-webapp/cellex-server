package com.example.cellex.models.shop;

import com.example.cellex.enums.ShopStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for the 'shops' table in PostgreSQL (Supabase).
 * Migrated from MongoDB @Document. Same package and class name
 * to preserve backward compatibility across 12+ dependent files.
 */
@Entity
@Table(name = "shops", indexes = {
        @Index(name = "idx_shops_owner_id", columnList = "owner_id"),
        @Index(name = "idx_shops_status", columnList = "status"),
        @Index(name = "idx_shops_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    /**
     * FK → users(id). Stored as UUID internally.
     * Backward-compat: getVendorId()/setVendorId(String) preserved.
     */
    @Column(name = "owner_id", nullable = false)
    @JsonIgnore
    private UUID ownerUuid;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    /**
     * Address stored as JSONB in PostgreSQL.
     * Hibernate 6 maps Java object ↔ JSONB automatically.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address_json", columnDefinition = "jsonb")
    private Address address;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ShopStatus status = ShopStatus.PENDING;

    @Column(name = "rating", precision = 3, scale = 2)
    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Backward-compatible ID accessors ====================

    /**
     * Returns the shop ID as String (backward compatible with MongoDB String id).
     */
    @JsonProperty("id")
    public String getId() {
        return uuid != null ? uuid.toString() : null;
    }

    /**
     * Sets the shop ID from a String (backward compatible).
     */
    public void setId(String id) {
        this.uuid = (id != null && !id.isBlank()) ? UUID.fromString(id) : null;
    }

    /**
     * Returns vendorId as String (backward compatible).
     * Maps to owner_id UUID column.
     */
    @JsonProperty("vendorId")
    public String getVendorId() {
        return ownerUuid != null ? ownerUuid.toString() : null;
    }

    /**
     * Sets vendorId from String (backward compatible).
     */
    public void setVendorId(String vendorId) {
        this.ownerUuid = (vendorId != null && !vendorId.isBlank()) ? UUID.fromString(vendorId) : null;
    }

    // ==================== Inner classes ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String street;
        private String commune;
        private String province;
        @Builder.Default
        private String country = "Việt Nam";
        private String fullAddress;
        @Builder.Default
        private boolean isDefault = false;
    }
}
