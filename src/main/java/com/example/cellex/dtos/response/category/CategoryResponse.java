package com.example.cellex.dtos.response.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private String id;
    private String name;
    private String slug;
    private String imageUrl;
    private String description;
    private Boolean isActive;
    private CategoryResponse parent; // Thêm field parent để map category cha
    private List<CategoryAttributeResponse> attributes; // Danh sách các thuộc tính của category
}
