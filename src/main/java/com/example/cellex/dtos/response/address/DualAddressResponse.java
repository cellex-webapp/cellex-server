package com.example.cellex.dtos.response.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Complete address response containing both old and new address formats
 * Used for displaying address in dual format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DualAddressResponse {
    
    /**
     * The stored new ward code (primary key for storage)
     */
    @JsonProperty("new_ward_code")
    private String newWardCode;
    
    /**
     * Detail address (street, house number, etc.)
     */
    @JsonProperty("detail_address")
    private String detailAddress;
    
    /**
     * New address format (after 07/2025)
     */
    @JsonProperty("new_address")
    private NewAddressDisplay newAddress;
    
    /**
     * Old address format (before 07/2025)
     * May contain multiple entries if the new ward was merged from multiple old wards
     */
    @JsonProperty("old_addresses")
    private List<OldAddressDisplay> oldAddresses;
    
    /**
     * Complete full address in new format
     */
    @JsonProperty("full_address_new")
    private String fullAddressNew;
    
    /**
     * Complete full address in old format (uses first matched old ward)
     */
    @JsonProperty("full_address_old")
    private String fullAddressOld;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewAddressDisplay {
        @JsonProperty("province_code")
        private String provinceCode;
        
        @JsonProperty("province_name")
        private String provinceName;
        
        @JsonProperty("ward_code")
        private String wardCode;
        
        @JsonProperty("ward_name")
        private String wardName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OldAddressDisplay {
        @JsonProperty("province_id")
        private String provinceId;
        
        @JsonProperty("province_name")
        private String provinceName;
        
        @JsonProperty("district_id")
        private String districtId;
        
        @JsonProperty("district_name")
        private String districtName;
        
        @JsonProperty("ward_id")
        private String wardId;
        
        @JsonProperty("ward_name")
        private String wardName;
    }
}
