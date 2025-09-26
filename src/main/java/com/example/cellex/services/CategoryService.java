package com.example.cellex.services;

import com.example.cellex.dtos.request.CategoryRequest;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.Category;
import com.example.cellex.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final S3Service s3Service;

    // CREATE
    public Category createCategory(CategoryRequest request, MultipartFile imageFile) throws IOException {
        String imageUrl = s3Service.uploadFile(imageFile);

        Category category = Category.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .imageUrl(imageUrl)
                .isActive(true) // Mặc định là true
                .build();

        return categoryRepository.save(category);
    }

    // READ ALL
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    // READ ONE
    public Category getCategoryById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    // UPDATE
    public Category updateCategory(String id, CategoryRequest request, MultipartFile imageFile) throws IOException {
        Category existingCategory = getCategoryById(id);

        // Upload ảnh mới nếu có
        String imageUrl = s3Service.uploadFile(imageFile);
        if (imageUrl != null) {
            existingCategory.setImageUrl(imageUrl);
        }

        existingCategory.setName(request.getName());
        existingCategory.setParentId(request.getParentId());

        return categoryRepository.save(existingCategory);
    }

    // DELETE (Soft delete)
    public void deleteCategory(String id) {
        Category category = getCategoryById(id);
        category.setIsActive(false); // Đánh dấu là không hoạt động thay vì xóa cứng
        categoryRepository.save(category);
    }
}