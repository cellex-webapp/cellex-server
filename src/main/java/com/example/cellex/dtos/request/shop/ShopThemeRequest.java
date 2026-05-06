package com.example.cellex.dtos.request.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for creating or updating ShopTheme (SDUI configuration).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopThemeRequest {

    @NotBlank(message = "Primary color is required")
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Primary color must be a valid hex color (e.g., #1677FF)")
    private String primaryColor;

    @NotBlank(message = "Secondary color is required")
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Secondary color must be a valid hex color (e.g., #FFFFFF)")
    private String secondaryColor;

    @NotBlank(message = "Font family is required")
    private String fontFamily;

    /**
     * Layout configuration as JSONB.
     * Example:
     * {
     *   "header": { "backgroundColor": "#F5F5F5", "height": 80 },
     *   "footer": { "show": true, "backgroundColor": "#333333" },
     *   "sections": [...]
     * }
     */
    private Map<String, Object> layoutConfig;

    private Boolean isPublished;
}
