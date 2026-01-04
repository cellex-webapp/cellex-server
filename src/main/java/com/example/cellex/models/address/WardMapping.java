package com.example.cellex.models.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for ward mapping between old and new address systems
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardMapping {
    
    private OldWardInfo old;
    
    @JsonProperty("new")
    private NewWardInfo newWard;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("updated_at")
    private String updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OldWardInfo {
        @JsonProperty("ward_code")
        private String wardCode;
        
        @JsonProperty("ward_name")
        private String wardName;
        
        @JsonProperty("district_name")
        private String districtName;
        
        @JsonProperty("province_name")
        private String provinceName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewWardInfo {
        @JsonProperty("ward_code")
        private String wardCode;
        
        @JsonProperty("ward_name")
        private String wardName;
        
        @JsonProperty("province_name")
        private String provinceName;
    }
}
