package com.example.cellex.dtos.response.warranty;

import com.example.cellex.enums.WarrantyStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyClaimResponse {
    private String id;
    private String orderItemId;
    private String userId;
    private String shopId;
    private WarrantyStatus status;
    private String issueDescription;
    private List<String> images;
    private String shopResponse;

    // Enriched fields for vendor dashboard
    private String userName;
    private String userEmail;
    private String productName;
    private String productImage;
    private String orderCode;

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}