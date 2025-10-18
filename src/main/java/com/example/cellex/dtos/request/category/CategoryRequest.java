package com.example.cellex.dtos.request.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Schema(description = "Name of the category", example = "Smartphones", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "URL-friendly slug for the category", example = "smartphones (optional)")
    @Size(max = 150, message = "Slug must not exceed 150 characters")
    private String slug;

    @Schema(description = "ID of the parent category (optional)", example = "60c72b2f9b1d8c001f8e4c8c")
    private String parentId;

    @Schema(description = "Description of the category", example = "Mobile phones and accessories")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Schema(description = "Set the active status of the category", example = "true")
    private Boolean isActive;
}
