package com.example.cellex.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CategoryRequest {
    @Schema(description = "Name of the category", example = "Smartphones")
    private String name;

    @Schema(description = "ID of the parent category (optional)", example = "60c72b2f9b1d8c001f8e4c8c")
    private String parentId;
}