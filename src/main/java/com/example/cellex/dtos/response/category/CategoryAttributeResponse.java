package com.example.cellex.dtos.response.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryAttributeResponse {

    private String id;
    private String categoryId;
    private String attributeName;
    private String attributeKey;
    private String dataType;
    private String unit;
    private Boolean isRequired;
    private Boolean isHighlight;
    private List<String> selectOptions;
    private String validationPattern;
    private Integer sortOrder;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
