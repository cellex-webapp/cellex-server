package com.example.cellex.models.coupon;

import com.example.cellex.enums.CampaignStatus;
import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.DistributionType;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "coupon_campaigns")
public class CouponCampaign {

    @Id
    private String id;

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("code_template")
    private String codeTemplate; // Nếu SHARED_CODE thì dùng code này, nếu UNIQUE thì null

    @Field("coupon_type")
    private CouponType couponType;

    @Field("discount_value")
    private Double discountValue;

    @Field("min_order_amount")
    private Double minOrderAmount;

    // Áp dụng cho sản phẩm/danh mục cụ thể
    @Field("applicable_product_ids")
    private List<String> applicableProductIds;

    @Field("applicable_category_ids")
    private List<String> applicableCategoryIds;

    @Field("start_date")
    private LocalDateTime startDate;

    @Field("end_date")
    private LocalDateTime endDate;

    @Field("distribution_type")
    private DistributionType distributionType;

    @Field("max_total_issuance")
    private Integer maxTotalIssuance; // Tổng số coupon tối đa

    @Field("per_user_limit")
    private Integer perUserLimit; // Số lần 1 user được nhận

    @Field("current_issuance")
    @Builder.Default
    private Integer currentIssuance = 0; // Số lượng đã phát

    @Field("status")
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Field("scheduled_at")
    private LocalDateTime scheduledAt; // Thời điểm phát theo lịch

    @Field("distributed_at")
    private LocalDateTime distributedAt; // Thời điểm thực sự phát

    @Field("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Field("created_by")
    private String createdBy; // Admin ID

    @Field("note")
    private String note;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}

