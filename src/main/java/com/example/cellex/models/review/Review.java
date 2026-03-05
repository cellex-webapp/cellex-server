package com.example.cellex.models.review;

import com.example.cellex.enums.ReviewStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "product_id", length = 50)
    private String productId;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_avatar", columnDefinition = "TEXT")
    private String userAvatar;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "order_item_id", length = 50)
    private String orderItemId;

    @Column(name = "shop_id", length = 50)
    private String shopId;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "jsonb")
    private List<String> images;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "videos", columnDefinition = "jsonb")
    private List<String> videos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vendor_response", columnDefinition = "jsonb")
    private VendorResponse vendorResponse;

    @Column(name = "is_verified_purchase")
    @Builder.Default
    private Boolean isVerifiedPurchase = true;

    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "helpful_voted_user_ids", columnDefinition = "jsonb")
    private List<String> helpfulVotedUserIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING_MODERATION;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "moderation_result", columnDefinition = "jsonb")
    private ModerationResult moderationResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "admin_decision", columnDefinition = "jsonb")
    private AdminDecision adminDecision;

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
