package com.example.cellex.dtos.request.livestream;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLivestreamRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    private String thumbnail;
}