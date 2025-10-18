package com.example.cellex.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "categories")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    @Id
    private String id;

    private String name;

    private String slug; // URL-friendly name

    private String parentId;

    private String imageUrl;

    private String description; // Mô tả chi tiết về danh mục

    @Builder.Default
    private Boolean isActive = true;
}