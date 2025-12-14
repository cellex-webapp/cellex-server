package com.example.cellex.models.review;

import com.example.cellex.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reviews")
public class Review {

    @Id
    private String id;

    @Indexed
    @Field("product_id")
    private String productId;

    @Indexed
    @Field("user_id")
    private String userId;

    @Field("user_name")
    private String userName;

    @Field("user_avatar")
    private String userAvatar;

    @Indexed
    @Field("order_id")
    private String orderId;

    @Field("order_item_id")
    private String orderItemId; // ID của item trong order để đánh giá đúng sản phẩm

    @Indexed
    @Field("shop_id")
    private String shopId;

    @Field("rating")
    private Integer rating; // 1-5 sao

    @Field("comment")
    private String comment; // Bình luận

    @Field("images")
    private List<String> images; // URLs của hình ảnh

    @Field("videos")
    private List<String> videos; // URLs của video

    @Field("vendor_response")
    private VendorResponse vendorResponse; // Phản hồi từ vendor

    @Field("is_verified_purchase")
    @Builder.Default
    private Boolean isVerifiedPurchase = true; // Luôn true vì chỉ cho phép review sau khi nhận hàng

    // Moderation fields
    @Indexed
    @Field("status")
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING_MODERATION;

    @Field("moderation_result")
    private ModerationResult moderationResult;

    @Field("admin_decision")
    private AdminDecision adminDecision;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
