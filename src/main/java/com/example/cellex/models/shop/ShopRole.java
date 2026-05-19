package com.example.cellex.models.shop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shop_roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_shop_roles_shop_name", columnNames = {"shop_id", "name"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopRole {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "shop_id", nullable = false)
    @JsonIgnore
    private UUID shopUuid;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "jsonb")
    private List<String> permissions;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("id")
    public String getId() {
        return uuid != null ? uuid.toString() : null;
    }

    public void setId(String id) {
        this.uuid = (id != null && !id.isBlank()) ? UUID.fromString(id) : null;
    }

    @JsonProperty("shopId")
    public String getShopId() {
        return shopUuid != null ? shopUuid.toString() : null;
    }

    public void setShopId(String shopId) {
        this.shopUuid = (shopId != null && !shopId.isBlank()) ? UUID.fromString(shopId) : null;
    }
}

