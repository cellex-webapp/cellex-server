package com.example.cellex.models.coupon;

import com.example.cellex.enums.CampaignStatus;
import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.DistributionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coupon_campaigns")
public class CouponCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "code_template", length = 100)
    private String codeTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", length = 50, nullable = false)
    private CouponType couponType;

    @Column(name = "discount_value")
    private Double discountValue;

    @Column(name = "min_order_amount")
    private Double minOrderAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_product_ids", columnDefinition = "jsonb")
    private List<String> applicableProductIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_category_ids", columnDefinition = "jsonb")
    private List<String> applicableCategoryIds;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_type", length = 50)
    private DistributionType distributionType;

    @Column(name = "max_total_issuance")
    private Integer maxTotalIssuance;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "current_issuance")
    @Builder.Default
    private Integer currentIssuance = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "distributed_at")
    private LocalDateTime distributedAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Backward-compatible ID accessors ====================

    @JsonProperty("id")
    public String getId() {
        return uuid != null ? uuid.toString() : null;
    }

    @JsonIgnore
    public void setId(String id) {
        this.uuid = id != null ? UUID.fromString(id) : null;
    }
}

