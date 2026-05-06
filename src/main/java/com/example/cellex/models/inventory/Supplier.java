package com.example.cellex.models.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_suppliers_shop_id", columnList = "shop_id"),
        @Index(name = "idx_suppliers_name", columnList = "supplier_name")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_suppliers_shop_phone", columnNames = {"shop_id", "phone_number"}),
        @UniqueConstraint(name = "uk_suppliers_shop_tax", columnNames = {"shop_id", "tax_code"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "shop_id", nullable = false)
    @JsonIgnore
    private UUID shopUuid;

    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "tax_code", nullable = false, length = 100)
    private String taxCode;

    @Column(name = "debt_amount", precision = 15, scale = 2, nullable = false)
    @JsonIgnore
    @Builder.Default
    private BigDecimal debtAmountDecimal = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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

    @JsonProperty("debtAmount")
    public Double getDebtAmount() {
        return debtAmountDecimal != null ? debtAmountDecimal.doubleValue() : 0.0;
    }

    public void setDebtAmount(Double debtAmount) {
        this.debtAmountDecimal = debtAmount != null ? BigDecimal.valueOf(debtAmount) : BigDecimal.ZERO;
    }
}
