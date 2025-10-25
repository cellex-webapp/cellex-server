package com.example.cellex.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter để chọn người nhận coupon campaign")
public class CampaignRecipientFilter {
    
    @Schema(description = "Phát cho tất cả users active", example = "false")
    private Boolean all;
    
    @Schema(description = "Filter theo segment ID")
    private String customerSegmentId;
    
    @Schema(description = "Filter theo tổng chi tiêu tối thiểu", example = "5000000")
    private Double minTotalSpend;
    
    @Schema(description = "Filter theo tổng chi tiêu tối đa", example = "10000000")
    private Double maxTotalSpend;
    
    @Schema(description = "Filter users đăng ký trước ngày này")
    private LocalDateTime registeredBefore;
    
    @Schema(description = "Filter users đăng ký sau ngày này")
    private LocalDateTime registeredAfter;
    
    @Schema(description = "Filter users mua hàng trong X ngày gần đây", example = "30")
    private Integer lastPurchaseDays;
    
    @Schema(description = "Filter theo tỉnh/thành phố")
    private String city;
    
    @Schema(description = "Filter theo quận/huyện")
    private String district;
    
    @Schema(description = "Filter users đã mua sản phẩm cụ thể")
    private List<String> hasPurchasedProductIds;
    
    @Schema(description = "Filter users đã mua danh mục cụ thể")
    private List<String> hasPurchasedCategoryIds;
    
    @Schema(description = "Danh sách user IDs cụ thể")
    private List<String> explicitUserIds;
    
    @Schema(description = "Loại trừ user IDs")
    private List<String> excludeUserIds;
}

