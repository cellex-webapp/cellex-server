package com.example.cellex.dtos.request.review;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VendorResponseRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String comment;
}
