package com.example.cellex.services;

import com.example.cellex.dtos.request.CategoryAttributeRequest;
import com.example.cellex.dtos.response.CategoryAttributeResponse;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.CategoryAttribute;
import com.example.cellex.repositories.CategoryAttributeRepository;
import com.example.cellex.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryAttributeService {

    private final CategoryAttributeRepository categoryAttributeRepository;
    private final CategoryRepository categoryRepository;

    public CategoryAttributeResponse createCategoryAttribute(String categoryId, CategoryAttributeRequest request) {
        // Kiểm tra category có tồn tại không
        if (!categoryRepository.existsById(categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTED);
        }

        // Kiểm tra attributeKey đã tồn tại trong category này chưa
        if (categoryAttributeRepository.existsByCategoryIdAndAttributeKey(categoryId, request.getAttributeKey())) {
            throw new AppException(ErrorCode.ATTRIBUTE_KEY_EXISTED);
        }

        // Validate selectOptions nếu dataType là SELECT hoặc MULTI_SELECT
        if (("SELECT".equals(request.getDataType()) || "MULTI_SELECT".equals(request.getDataType()))
            && (request.getSelectOptions() == null || request.getSelectOptions().isEmpty())) {
            throw new AppException(ErrorCode.SELECT_OPTIONS_REQUIRED);
        }

        CategoryAttribute categoryAttribute = CategoryAttribute.builder()
                .categoryId(categoryId)
                .attributeName(request.getAttributeName())
                .attributeKey(request.getAttributeKey())
                .dataType(request.getDataType())
                .unit(request.getUnit())
                .isRequired(request.getIsRequired())
                .isHighlight(request.getIsHighlight())
                .selectOptions(request.getSelectOptions())
                .validationPattern(request.getValidationPattern())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .description(request.getDescription())
                .build();

        CategoryAttribute savedAttribute = categoryAttributeRepository.save(categoryAttribute);
        log.info("Created category attribute: {} for category: {}", savedAttribute.getId(), categoryId);

        return mapToResponse(savedAttribute);
    }

    public List<CategoryAttributeResponse> getCategoryAttributes(String categoryId) {
        List<CategoryAttribute> attributes = categoryAttributeRepository
                .findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(categoryId);

        return attributes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryAttributeResponse> getHighlightAttributes(String categoryId) {
        List<CategoryAttribute> attributes = categoryAttributeRepository
                .findByCategoryIdAndIsHighlightTrueAndIsActiveTrueOrderBySortOrderAsc(categoryId);

        return attributes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CategoryAttributeResponse updateCategoryAttribute(String attributeId, CategoryAttributeRequest request) {
        CategoryAttribute attribute = categoryAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_ATTRIBUTE_NOT_FOUND));

        // Kiểm tra attributeKey mới có trùng với attribute khác trong cùng category không
        if (!attribute.getAttributeKey().equals(request.getAttributeKey()) &&
            categoryAttributeRepository.existsByCategoryIdAndAttributeKey(attribute.getCategoryId(), request.getAttributeKey())) {
            throw new AppException(ErrorCode.ATTRIBUTE_KEY_EXISTED);
        }

        // Validate selectOptions
        if (("SELECT".equals(request.getDataType()) || "MULTI_SELECT".equals(request.getDataType()))
            && (request.getSelectOptions() == null || request.getSelectOptions().isEmpty())) {
            throw new AppException(ErrorCode.SELECT_OPTIONS_REQUIRED);
        }

        attribute.setAttributeName(request.getAttributeName());
        attribute.setAttributeKey(request.getAttributeKey());
        attribute.setDataType(request.getDataType());
        attribute.setUnit(request.getUnit());
        attribute.setIsRequired(request.getIsRequired());
        attribute.setIsHighlight(request.getIsHighlight());
        attribute.setSelectOptions(request.getSelectOptions());
        attribute.setValidationPattern(request.getValidationPattern());
        attribute.setSortOrder(request.getSortOrder());
        attribute.setDescription(request.getDescription());
        attribute.setUpdatedAt(LocalDateTime.now());

        CategoryAttribute savedAttribute = categoryAttributeRepository.save(attribute);
        log.info("Updated category attribute: {}", attributeId);

        return mapToResponse(savedAttribute);
    }

    public void deleteCategoryAttribute(String attributeId) {
        CategoryAttribute attribute = categoryAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_ATTRIBUTE_NOT_FOUND));

        attribute.setIsActive(false);
        attribute.setUpdatedAt(LocalDateTime.now());
        categoryAttributeRepository.save(attribute);

        log.info("Deleted (deactivated) category attribute: {}", attributeId);
    }

    private CategoryAttributeResponse mapToResponse(CategoryAttribute attribute) {
        return CategoryAttributeResponse.builder()
                .id(attribute.getId())
                .categoryId(attribute.getCategoryId())
                .attributeName(attribute.getAttributeName())
                .attributeKey(attribute.getAttributeKey())
                .dataType(attribute.getDataType())
                .unit(attribute.getUnit())
                .isRequired(attribute.getIsRequired())
                .isHighlight(attribute.getIsHighlight())
                .selectOptions(attribute.getSelectOptions())
                .validationPattern(attribute.getValidationPattern())
                .sortOrder(attribute.getSortOrder())
                .description(attribute.getDescription())
                .isActive(attribute.getIsActive())
                .createdAt(attribute.getCreatedAt())
                .updatedAt(attribute.getUpdatedAt())
                .build();
    }
}
