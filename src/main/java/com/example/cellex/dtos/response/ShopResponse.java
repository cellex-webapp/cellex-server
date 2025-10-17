package com.example.cellex.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopResponse {

    private String id;

    @JsonProperty("vendor_id")
    private String vendorId;

    @JsonProperty("shop_name")
    private String shopName;

    private String description;

    @JsonProperty("logo_url")
    private String logoUrl;

    private String address;

    @JsonProperty("phone_number")
    private String phoneNumber;

    private String email;

    @JsonProperty("is_verified")
    private Boolean isVerified;

    private Double rating;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
