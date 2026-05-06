package com.example.cellex.dtos.response.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for ShopTheme (SDUI configuration).
 * Exposes the theme configuration for Shop UI rendering.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopThemeResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("shopId")
    private String shopId;

    @JsonProperty("primaryColor")
    private String primaryColor;

    @JsonProperty("secondaryColor")
    private String secondaryColor;

    @JsonProperty("fontFamily")
    private String fontFamily;

    @JsonProperty("layoutConfig")
    private Map<String, Object> layoutConfig;

    @JsonProperty("isPublished")
    private Boolean isPublished;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
