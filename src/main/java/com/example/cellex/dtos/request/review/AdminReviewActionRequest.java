package com.example.cellex.dtos.request.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminReviewActionRequest {

    @Schema(description = "Lý do thực hiện hành động", example = "Nội dung đã được xác minh là phù hợp")
    @NotBlank(message = "Lý do không được để trống")
    private String reason;
}
