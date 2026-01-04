package com.example.cellex.models.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for old address system ward (before 07/2025)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OldWard {
    
    @JsonProperty("level3_id")
    private String id;
    
    private String name;
    
    private String type;
}
