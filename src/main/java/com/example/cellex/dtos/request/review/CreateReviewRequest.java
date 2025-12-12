package com.example.cellex.dtos.request.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class CreateReviewRequest {

    @Schema(description = "Order ID", example = "string", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Order ID không được để trống")
    @JsonProperty("order_id")
    private String orderId;

    @Schema(description = "Product ID", example = "string", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product ID không được để trống")
    @JsonProperty("product_id")
    private String productId;

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
