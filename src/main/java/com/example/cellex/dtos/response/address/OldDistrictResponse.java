package com.example.cellex.dtos.response.address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple response DTO for district in old address system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OldDistrictResponse {
    
    private String id;
    
    private String name;
    
    private String type;
    
    private String provinceId;
}
