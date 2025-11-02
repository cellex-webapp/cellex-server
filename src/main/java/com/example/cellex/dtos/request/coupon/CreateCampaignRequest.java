package com.example.cellex.dtos.request.coupon;

import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.DistributionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request tạo campaign coupon mới")
public class CreateCampaignRequest {
    
    @Schema(description = "Tiêu đề campaign", example = "Flash Sale Tết 2025", required = true)
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    @Schema(description = "Mô tả campaign", example = "Khuyến mãi đặc biệt dịp Tết")
    private String description;
    
    @Schema(description = "Code template cho SHARED_CODE, null nếu UNIQUE_PER_USER", example = "TET2025")
    private String codeTemplate;
    
    @Schema(description = "Loại coupon", example = "PERCENTAGE", allowableValues = {"PERCENTAGE", "FIXED", "FREE_SHIPPING"}, required = true)
    @NotNull(message = "Loại coupon không được để trống")
    private CouponType couponType;
    
    @Schema(description = "Giá trị giảm", example = "20", required = true)
    @NotNull(message = "Giá trị giảm không được để trống")
    @Min(value = 0, message = "Giá trị phải >= 0")
    private Double discountValue;
    
    @Schema(description = "Giá trị đơn hàng tối thiểu", example = "500000")
    @Min(value = 0, message = "Giá trị phải >= 0")
    private Double minOrderAmount;
    
    @Schema(description = "Áp dụng cho sản phẩm cụ thể (Product IDs)")
    private List<String> applicableProductIds;
    
    @Schema(description = "Áp dụng cho danh mục cụ thể (Category IDs)")
    private List<String> applicableCategoryIds;
    
    @Schema(description = "Ngày bắt đầu hiệu lực", example = "2025-01-20T00:00:00", required = true)
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;
    
    @Schema(description = "Ngày kết thúc hiệu lực", example = "2025-01-31T23:59:59", required = true)
    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;
    
    @Schema(description = "Kiểu phát coupon", example = "UNIQUE_PER_USER", allowableValues = {"SHARED_CODE", "UNIQUE_PER_USER"}, required = true)
    @NotNull(message = "Kiểu phát không được để trống")
    private DistributionType distributionType;
    
    @Schema(description = "Tổng số coupon tối đa", example = "1000")
    @Min(value = 1, message = "Phải >= 1")
    private Integer maxTotalIssuance;
    
    @Schema(description = "Số lần 1 user được nhận", example = "1")
    @Min(value = 1, message = "Phải >= 1")
    private Integer perUserLimit;
    
    @Schema(description = "Lập lịch phát vào thời điểm cụ thể (null = phát ngay)", example = "2025-01-20T09:00:00")
    private LocalDateTime scheduledAt;
    
    @Schema(description = "Ghi chú")
    private String note;
}

