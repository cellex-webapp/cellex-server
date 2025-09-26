package com.example.cellex.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các trường null trong JSON
public class CategoryResponse {
    private String id;
    private String name;
    private String imageUrl;
    private boolean isActive;
    private CategoryResponse parent; // Trả về cả object cha
}