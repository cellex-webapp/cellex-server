package com.example.cellex.services.category;

import com.example.cellex.dtos.request.category.CategoryRequest;
import com.example.cellex.dtos.response.category.CategoryAttributeResponse;
import com.example.cellex.dtos.response.category.CategoryResponse;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.category.Category;
import com.example.cellex.models.category.CategoryAttribute;
import com.example.cellex.repositories.category.CategoryRepository;
import com.example.cellex.repositories.category.CategoryAttributeRepository;
import com.example.cellex.services.S3Service;
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
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final S3Service s3Service;

    // CREATE
    public CategoryResponse createCategory(CategoryRequest request, MultipartFile imageFile) throws IOException {
        String imageUrl = s3Service.uploadFile(imageFile, "categories");

        // Tự động tạo slug nếu không có
        String slug = request.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlugFromName(request.getName());
        } else {
            slug = normalizeSlug(slug);
        }

        // Kiểm tra slug có trùng không
        if (categoryRepository.existsBySlug(slug)) {
            slug = generateUniqueSlug(slug);
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .parentId(request.getParentId())
                .imageUrl(imageUrl)
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(savedCategory);
    }

    // CREATE MULTIPART - Phương thức mới để hỗ trợ multipart form data
    public CategoryResponse createCategoryMultipart(String name, String description, String parentId,
                                                   Boolean isActive, MultipartFile imageFile) throws IOException {
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3Service.uploadFile(imageFile, "categories");
        }

        // Tự động tạo slug từ tên
        String slug = generateSlugFromName(name);

        // Kiểm tra slug có trùng không
        if (categoryRepository.existsBySlug(slug)) {
            slug = generateUniqueSlug(slug);
        }

        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .parentId(parentId)
                .imageUrl(imageUrl)
                .description(description)
                .isActive(isActive != null ? isActive : true)
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

    public org.springframework.data.domain.Page<CategoryResponse> getAllActiveCategories(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Category> page = categoryRepository.findByIsActiveTrue(pageable);
        return page.map(this::mapToCategoryResponse);
    }

    // READ ONE
    public CategoryResponse getCategoryById(String id) {
        Category category = findCategoryByIdInternal(id);
        return mapToCategoryResponse(category);
    }

    // READ ONE BY SLUG
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return mapToCategoryResponse(category);
    }

    // UPDATE
    public CategoryResponse updateCategory(String id, CategoryRequest request, MultipartFile imageFile) throws IOException {
        Category category = findCategoryByIdInternal(id);

        // Update name if provided
        if (StringUtils.hasText(request.getName())) {
            category.setName(request.getName());
        }

        // Update slug if provided
        if (StringUtils.hasText(request.getSlug())) {
            String newSlug = normalizeSlug(request.getSlug());
            // Kiểm tra slug có trùng với category khác không (trừ chính nó)
            if (!category.getSlug().equals(newSlug) && categoryRepository.existsBySlug(newSlug)) {
                newSlug = generateUniqueSlug(newSlug);
            }
            category.setSlug(newSlug);
        }

        // Update description if provided
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        // Update parent ID if provided
        if (request.getParentId() != null) {
            category.setParentId(request.getParentId());
        }

        // Update isActive if provided
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
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

    // UPDATE MULTIPART - Phương thức mới để hỗ trợ multipart form data
    public CategoryResponse updateCategoryMultipart(String id, String name, String description, String parentId,
                                                   Boolean isActive, MultipartFile imageFile) throws IOException {
        Category category = findCategoryByIdInternal(id);

        // Update name if provided
        if (StringUtils.hasText(name)) {
            category.setName(name);
            // Tạo lại slug từ tên mới
            String newSlug = generateSlugFromName(name);
            if (!category.getSlug().equals(newSlug) && categoryRepository.existsBySlug(newSlug)) {
                newSlug = generateUniqueSlug(newSlug);
            }
            category.setSlug(newSlug);
        }

        // Update description if provided
        if (description != null) {
            category.setDescription(description);
        }

        // Update parent ID if provided
        if (parentId != null) {
            category.setParentId(parentId);
        }

        // Update isActive if provided
        if (isActive != null) {
            category.setIsActive(isActive);
        }

        // Update image if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            // Xóa ảnh cũ nếu có
            if (StringUtils.hasText(category.getImageUrl())) {
                s3Service.deleteFile(category.getImageUrl());
            }
            // Upload ảnh mới
            String newImageUrl = s3Service.uploadFile(imageFile, "categories");
            category.setImageUrl(newImageUrl);
        }

        Category updatedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(updatedCategory);
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
                .slug(category.getSlug())
                .imageUrl(category.getImageUrl())
                .description(category.getDescription())
                .isActive(category.getIsActive());

        // If has parentId, find and recursively map parent object
        if (StringUtils.hasText(category.getParentId())) {
            categoryRepository.findById(category.getParentId()).ifPresent(parentEntity -> {
                builder.parent(mapToCategoryResponse(parentEntity));
            });
        }

        // Load and map category attributes
        List<CategoryAttribute> attributes = categoryAttributeRepository.findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(category.getId());
        List<CategoryAttributeResponse> attributeResponses = attributes.stream()
                .map(attr -> CategoryAttributeResponse.builder()
                        .id(attr.getId())
                        .categoryId(attr.getCategoryId())
                        .attributeName(attr.getAttributeName())
                        .attributeKey(attr.getAttributeKey())
                        .dataType(attr.getDataType())
                        .unit(attr.getUnit())
                        .isRequired(attr.getIsRequired())
                        .isHighlight(attr.getIsHighlight())
                        .selectOptions(attr.getSelectOptions())
                        .validationPattern(attr.getValidationPattern())
                        .sortOrder(attr.getSortOrder())
                        .description(attr.getDescription())
                        .isActive(attr.getIsActive())
                        .createdAt(attr.getCreatedAt())
                        .updatedAt(attr.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
        builder.attributes(attributeResponses);

        return builder.build();
    }

    // Tạo slug tự động từ name
    private String generateSlugFromName(String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-|-$", ""); // Xóa dấu - ở đầu và cuối
        return slug;
    }

    // Chuẩn hóa slug
    private String normalizeSlug(String slug) {
        // Ví dụ: chuyển đổi về dạng thường, xóa khoảng trắng, ký tự đặc biệt, ...
        slug = slug.toLowerCase().trim();
        slug = slug.replaceAll("[^a-z0-9-]", ""); // Giữ chỉ chữ cái, số và dấu -
        slug = slug.replaceAll("-+", "-"); // Thay thế nhiều dấu - liên tiếp bằng 1 dấu -
        slug = slug.replaceAll("^-|-$", ""); // Xóa dấu - ở đầu và cuối
        return slug;
    }

    // Tạo slug duy nhất
    private String generateUniqueSlug(String slug) {
        int count = 1;
        String newSlug = slug;
        while (categoryRepository.existsBySlug(newSlug)) {
            newSlug = slug + "-" + count;
            count++;
        }
        return newSlug;
    }
}
