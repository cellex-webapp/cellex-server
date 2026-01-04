package com.example.cellex.dtos.response.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for ward mapping between old and new address systems
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardMappingResponse {
    
    /**
     * The input ward code type: "old" or "new"
     */
    @JsonProperty("input_type")
    private String inputType;
    
    /**
     * Old address information (if found)
     */
    @JsonProperty("old_address")
    private OldAddressInfo oldAddress;
    
    /**
     * New address information (if found)
     */
    @JsonProperty("new_address")
    private NewAddressInfo newAddress;
    
    /**
     * List of old wards that map to the new ward (when input is new ward code)
     * This handles the case where multiple old wards merge into one new ward
     */
    @JsonProperty("old_wards_merged")
    private List<OldAddressInfo> oldWardsMerged;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OldAddressInfo {
        @JsonProperty("ward_code")
        private String wardCode;
        
        @JsonProperty("ward_name")
        private String wardName;
        
        @JsonProperty("district_name")
        private String districtName;
        
        @JsonProperty("province_name")
        private String provinceName;
        
        @JsonProperty("full_address")
        private String fullAddress;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewAddressInfo {
        @JsonProperty("ward_code")
        private String wardCode;
        
        @JsonProperty("ward_name")
        private String wardName;
        
        @JsonProperty("province_name")
        private String provinceName;
        
        @JsonProperty("full_address")
        private String fullAddress;
    }
}
