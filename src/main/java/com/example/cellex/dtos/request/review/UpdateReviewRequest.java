package com.example.cellex.dtos.request.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateReviewRequest {

    @Schema(description = "Rating (1-5 stars)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating phải từ 1 đến 5 sao")
    @Max(value = 5, message = "Rating phải từ 1 đến 5 sao")
    private Integer rating;

    @Schema(description = "Review comment", example = "string")
    private String comment;

    @Schema(description = "Image URLs (JSON array of strings)", example = "[\"string\"]")
    private List<String> images;

    @Schema(description = "Video URLs (JSON array of strings)", example = "[\"string\"]")
    private List<String> videos;
}
