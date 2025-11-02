package com.example.cellex.dtos.request.coupon;

import com.example.cellex.enums.CampaignStatus;
import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.DistributionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request cập nhật campaign (tất cả trường đều optional)")
public class UpdateCampaignRequest {
    
    @Schema(description = "Tiêu đề campaign")
    private String title;
    
    @Schema(description = "Mô tả campaign")
    private String description;
    
    @Schema(description = "Code template")
    private String codeTemplate;
    
    @Schema(description = "Loại coupon", allowableValues = {"PERCENTAGE", "FIXED", "FREE_SHIPPING"})
    private CouponType couponType;
    
    @Schema(description = "Giá trị giảm")
    @Min(value = 0, message = "Giá trị phải >= 0")
    private Double discountValue;
    
    @Schema(description = "Giá trị đơn hàng tối thiểu")
    @Min(value = 0, message = "Giá trị phải >= 0")
    private Double minOrderAmount;
    
    @Schema(description = "Áp dụng cho sản phẩm cụ thể")
    private List<String> applicableProductIds;
    
    @Schema(description = "Áp dụng cho danh mục cụ thể")
    private List<String> applicableCategoryIds;
    
    @Schema(description = "Ngày bắt đầu hiệu lực")
    private LocalDateTime startDate;
    
    @Schema(description = "Ngày kết thúc hiệu lực")
    private LocalDateTime endDate;
    
    @Schema(description = "Kiểu phát coupon", allowableValues = {"SHARED_CODE", "UNIQUE_PER_USER"})
    private DistributionType distributionType;
    
    @Schema(description = "Tổng số coupon tối đa")
    @Min(value = 1, message = "Phải >= 1")
    private Integer maxTotalIssuance;
    
    @Schema(description = "Số lần 1 user được nhận")
    @Min(value = 1, message = "Phải >= 1")
    private Integer perUserLimit;
    
    @Schema(description = "Lập lịch phát")
    private LocalDateTime scheduledAt;
    
    @Schema(description = "Trạng thái campaign", allowableValues = {"DRAFT", "SCHEDULED", "ACTIVE", "COMPLETED", "CANCELLED"})
    private CampaignStatus status;
    
    @Schema(description = "Trạng thái active")
    private Boolean isActive;
    
    @Schema(description = "Ghi chú")
    private String note;
}

