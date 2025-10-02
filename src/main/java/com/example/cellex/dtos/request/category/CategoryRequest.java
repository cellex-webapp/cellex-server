package com.example.cellex.dtos.request.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Schema(description = "Name of the category", example = "Smartphones", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "ID of the parent category (optional)", example = "60c72b2f9b1d8c001f8e4c8c")
    private String parentId;

    @Schema(description = "Set the active status of the category", example = "true")
    private Boolean isActive;
}
