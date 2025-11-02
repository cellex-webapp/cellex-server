package com.example.cellex.dtos.response.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDistributionResponse {
    private String id;
    private String campaignId;
    private String campaignTitle;
    private String adminId;
    private Map<String, Object> filterCriteria;
    private Integer recipientsCount;
    private Integer successCount;
    private Integer failedCount;
    private String errorSummary;
    private Long executionTimeMs;
    private LocalDateTime createdAt;
}

