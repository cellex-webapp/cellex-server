package com.example.cellex.models.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model for old address system province (before 07/2025)
 * Structure: Province -> District -> Ward (3 levels)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OldProvince {
    
    @JsonProperty("level1_id")
    private String id;
    
    private String name;
    
    private String type;
    
    @JsonProperty("level2s")
    private List<OldDistrict> districts;
}
