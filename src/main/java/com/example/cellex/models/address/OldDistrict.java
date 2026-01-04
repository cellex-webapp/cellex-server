package com.example.cellex.models.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model for old address system district (before 07/2025)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OldDistrict {
    
    @JsonProperty("level2_id")
    private String id;
    
    private String name;
    
    private String type;
    
    @JsonProperty("level3s")
    private List<OldWard> wards;
}
