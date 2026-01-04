package com.example.cellex.dtos.response.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple response DTO for province in new address system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewProvinceResponse {
    
    @JsonProperty("province_code")
    private String provinceCode;
    
    private String name;
    
    @JsonProperty("short_name")
    private String shortName;
    
    private String code;
    
    @JsonProperty("place_type")
    private String placeType;
}
