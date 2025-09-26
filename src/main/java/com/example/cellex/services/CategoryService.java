package com.example.cellex.services;

import com.example.cellex.dtos.request.CategoryRequest;
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
        String imageUrl = s3Service.uploadFile(imageFile);

        Category category = Category.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .imageUrl(imageUrl)
                .isActive(true) // Mặc định khi tạo mới
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
        Category existingCategory = findCategoryByIdInternal(id);

        String imageUrl = s3Service.uploadFile(imageFile);
        if (imageUrl != null) {
            existingCategory.setImageUrl(imageUrl);
        }

        existingCategory.setName(request.getName());
        existingCategory.setParentId(request.getParentId());

        // Cho phép cập nhật isActive
        if (request.getIsActive() != null) {
            existingCategory.setIsActive(request.getIsActive());
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        return mapToCategoryResponse(updatedCategory);
    }

    // DELETE (Hard delete)
    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        // Xóa vĩnh viễn
        categoryRepository.deleteById(id);
    }

    // Helper method để tìm category, tránh lặp code
    private Category findCategoryByIdInternal(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    // Helper method để map từ Entity sang Response DTO
    private CategoryResponse mapToCategoryResponse(Category category) {
        if (category == null) return null;

        CategoryResponse.CategoryResponseBuilder builder = CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .imageUrl(category.getImageUrl())
                .isActive(category.getIsActive());

        // Nếu có parentId, tìm và đệ quy map object cha
        if (StringUtils.hasText(category.getParentId())) {
            categoryRepository.findById(category.getParentId()).ifPresent(parentEntity -> {
                builder.parent(mapToCategoryResponse(parentEntity));
            });
        }

        return builder.build();
    }
}