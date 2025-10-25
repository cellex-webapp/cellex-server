package com.example.cellex.dtos.response;

import com.example.cellex.enums.CampaignStatus;
import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.DistributionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponCampaignResponse {
    private String id;
    private String title;
    private String description;
    private String codeTemplate;
    private CouponType couponType;
    private Double discountValue;
    private Double minOrderAmount;
    private List<String> applicableProductIds;
    private List<String> applicableCategoryIds;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private DistributionType distributionType;
    private Integer maxTotalIssuance;
    private Integer perUserLimit;
    private Integer currentIssuance;
    private CampaignStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime distributedAt;
    private Boolean isActive;
    private String createdBy;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

