package com.example.cellex.services;

import com.example.cellex.dtos.request.category.CategoryRequest;
import com.example.cellex.dtos.response.CategoryResponse;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.Category;
import com.example.cellex.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final S3Service s3Service;

    // CREATE
    public CategoryResponse createCategory(CategoryRequest request, MultipartFile imageFile) throws IOException {
        String imageUrl = s3Service.uploadFile(imageFile, "categories");

        Category category = Category.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .imageUrl(imageUrl)
                .isActive(true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(savedCategory);
    }

    // READ ALL
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    // READ ONE
    public CategoryResponse getCategoryById(String id) {
        Category category = findCategoryByIdInternal(id);
        return mapToCategoryResponse(category);
    }

    // UPDATE
    public CategoryResponse updateCategory(String id, CategoryRequest request, MultipartFile imageFile) throws IOException {
        Category category = findCategoryByIdInternal(id);

        // Update name if provided
        if (StringUtils.hasText(request.getName())) {
            category.setName(request.getName());
        }

        // Update parent ID if provided
        if (request.getParentId() != null) {
            category.setParentId(request.getParentId());
        }

        // Update image if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete old image if exists
            if (StringUtils.hasText(category.getImageUrl())) {
                s3Service.deleteFile(category.getImageUrl());
            }

            // Upload new image
            String newImageUrl = s3Service.uploadFile(imageFile, "categories");
            category.setImageUrl(newImageUrl);
        }

        Category savedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(savedCategory);
    }

    // DELETE (Hard delete)
    public void deleteCategory(String id) {
        Category category = findCategoryByIdInternal(id);

        // Delete image from S3 if exists
        if (StringUtils.hasText(category.getImageUrl())) {
            s3Service.deleteFile(category.getImageUrl());
        }

        // Delete permanently
        categoryRepository.deleteById(id);
    }

    // Helper method to find category, avoid code duplication
    private Category findCategoryByIdInternal(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    // Upload/Update category image
    public CategoryResponse uploadCategoryImage(String id, MultipartFile imageFile) throws IOException {
        Category category = findCategoryByIdInternal(id);

        if (imageFile == null || imageFile.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Delete old image if exists
        if (StringUtils.hasText(category.getImageUrl())) {
            s3Service.deleteFile(category.getImageUrl());
        }

        // Upload new image
        String newImageUrl = s3Service.uploadFile(imageFile, "categories");
        category.setImageUrl(newImageUrl);

        Category savedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(savedCategory);
    }

    // Helper method to map from Entity to Response DTO
    private CategoryResponse mapToCategoryResponse(Category category) {
        if (category == null) return null;

        CategoryResponse.CategoryResponseBuilder builder = CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .imageUrl(category.getImageUrl())
                .isActive(category.getIsActive());

        // If has parentId, find and recursively map parent object
        if (StringUtils.hasText(category.getParentId())) {
            categoryRepository.findById(category.getParentId()).ifPresent(parentEntity -> {
                builder.parent(mapToCategoryResponse(parentEntity));
            });
        }

        return builder.build();
    }
}
