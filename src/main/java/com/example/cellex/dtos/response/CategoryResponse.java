package com.example.cellex.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private String id;
    private String name;
    private String parentId;
    private String imageUrl;
    private Boolean isActive;
    private CategoryResponse parent; // Thêm field parent để map category cha
}
