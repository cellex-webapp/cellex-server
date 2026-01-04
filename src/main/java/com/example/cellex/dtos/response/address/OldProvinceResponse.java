package com.example.cellex.dtos.response.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple response DTO for province in old address system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OldProvinceResponse {
    
    private String id;
    
    private String name;
    
    private String type;
}
