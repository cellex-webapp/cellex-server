package com.example.cellex.dtos.response.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressResponse {

    private String id;

    @JsonProperty("user_id")
    private String userId;

    private String label;

    private String street; // detailAddress

    private String commune; // communeName

    private String province; // provinceName

    @JsonProperty("province_code")
    private String provinceCode;

    @JsonProperty("commune_code")
    private String communeCode;

    @Builder.Default
    private String country = "Việt Nam";

    @JsonProperty("full_address")
    private String fullAddress;

    @JsonProperty("default")
    private Boolean isDefault;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
