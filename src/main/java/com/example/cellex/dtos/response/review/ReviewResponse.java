package com.example.cellex.dtos.response.review;

import com.example.cellex.enums.ReviewStatus;
import com.example.cellex.models.review.AdminDecision;
import com.example.cellex.models.review.ModerationResult;
import com.example.cellex.models.review.VendorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {

    private String id;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("user_avatar")
    private String userAvatar;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("shop_id")
    private String shopId;

    private Integer rating;

    private String comment;

    private List<String> images;

    private List<String> videos;

    @JsonProperty("vendor_response")
    private VendorResponse vendorResponse;

    @JsonProperty("is_verified_purchase")
    private Boolean isVerifiedPurchase;

    // Moderation fields
    private ReviewStatus status;

    @JsonProperty("moderation_result")
    private ModerationResult moderationResult;

    @JsonProperty("admin_decision")
    private AdminDecision adminDecision;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    @JsonProperty("flagged_categories_vi")
    private List<String> flaggedCategoriesVi;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
