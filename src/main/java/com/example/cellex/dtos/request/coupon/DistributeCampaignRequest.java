package com.example.cellex.dtos.request.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request phát coupon campaign")
public class DistributeCampaignRequest {
    
    @Schema(description = "ID campaign cần phát", required = true)
    @NotBlank(message = "Campaign ID không được để trống")
    private String campaignId;
    
    @Schema(description = "Filter người nhận", required = true)
    @NotNull(message = "Filter không được để trống")
    @Valid
    private CampaignRecipientFilter filter;
}

