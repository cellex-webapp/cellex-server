package com.example.cellex.models.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for new address system ward (after 07/2025)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewWard {
    
    @JsonProperty("ward_code")
    private String wardCode;
    
    private String name;
    
    @JsonProperty("province_code")
    private String provinceCode;
}
