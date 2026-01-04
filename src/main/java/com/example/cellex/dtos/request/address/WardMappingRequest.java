package com.example.cellex.dtos.request.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for mapping ward codes between old and new address systems
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardMappingRequest {
    
    @NotBlank(message = "Ward code is required")
    @JsonProperty("ward_code")
    private String wardCode;
    
    /**
     * Type of the input ward code: "old" or "new"
     * If not specified, the system will auto-detect
     */
    @JsonProperty("code_type")
    private String codeType; // "old" or "new" or null for auto-detect
}
