package com.example.cellex.models.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model for new address system province (after 07/2025)
 * Structure: Province -> Ward (2 levels, no district)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewProvince {
    
    @JsonProperty("province_code")
    private String provinceCode;
    
    private String name;
    
    @JsonProperty("short_name")
    private String shortName;
    
    private String code;
    
    @JsonProperty("place_type")
    private String placeType;
    
    private List<NewWard> wards;
}
