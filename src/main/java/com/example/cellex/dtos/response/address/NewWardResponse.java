package com.example.cellex.dtos.response.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple response DTO for ward in new address system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewWardResponse {
    
    @JsonProperty("ward_code")
    private String wardCode;
    
    private String name;
    
    @JsonProperty("province_code")
    private String provinceCode;
}
