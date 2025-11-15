package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.category.CategoryResponse;
import com.example.cellex.services.category.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "09. Category Management", description = "APIs for creating, reading, updating, and deleting product categories.")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    // CREATE - Multipart Form Data
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Create a new category",
        description = "Creates a new category using multipart form data with optional image upload."
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(
            @Parameter(description = "Category name", required = true)
            @RequestParam("name") @NotBlank String name,

            @Parameter(description = "Category description")
            @RequestParam(value = "description", required = false) String description,

            @Parameter(description = "Parent category ID")
            @RequestParam(value = "parentId", required = false) String parentId,

            @Parameter(description = "Category active status")
            @RequestParam(value = "isActive", required = false) Boolean isActive,

            @Parameter(description = "Category image file")
            @RequestParam(value = "image", required = false) MultipartFile imageFile) throws IOException {

        CategoryResponse newCategory = categoryService.createCategoryMultipart(
                name, description, parentId, isActive, imageFile);
        return ApiResponse.<CategoryResponse>builder()
                .result(newCategory)
                .message("Category created successfully.")
                .build();
    }

    // READ ALL
        @GetMapping
        @Operation(summary = "Get all active categories", description = "Retrieves a list of all active categories with nested parent objects.")
        public ApiResponse<com.example.cellex.dtos.response.PageResponse<CategoryResponse>> getAllActiveCategories(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortType
        ) {
        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));

        Page<CategoryResponse> pageEntity = categoryService.getAllActiveCategories(pageable);
        com.example.cellex.dtos.response.PageResponse<CategoryResponse> pageResp = com.example.cellex.dtos.response.PageResponse.of(pageEntity);

        return ApiResponse.<com.example.cellex.dtos.response.PageResponse<CategoryResponse>>builder()
            .result(pageResp)
            .build();
        }

    // READ ONE
    @GetMapping("/{id}")
    @Operation(summary = "Get a category by ID")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable String id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ApiResponse.<CategoryResponse>builder()
                .result(category)
                .build();
    }

    // READ ONE BY SLUG
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get a category by slug", description = "Retrieves a category by its URL-friendly slug")
    public ApiResponse<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ApiResponse.<CategoryResponse>builder()
                .result(category)
                .message("Category retrieved successfully by slug.")
                .build();
    }

    // UPDATE - Multipart Form Data
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Update an existing category",
        description = "Updates category details using multipart form data with optional image upload."
    )
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable String id,

            @Parameter(description = "Category name")
            @RequestParam(value = "name", required = false) String name,

            @Parameter(description = "Category description")
            @RequestParam(value = "description", required = false) String description,

            @Parameter(description = "Parent category ID")
            @RequestParam(value = "parentId", required = false) String parentId,

            @Parameter(description = "Category active status")
            @RequestParam(value = "isActive", required = false) Boolean isActive,

            @Parameter(description = "Category image file")
            @RequestParam(value = "image", required = false) MultipartFile imageFile) throws IOException {

        CategoryResponse updatedCategory = categoryService.updateCategoryMultipart(
                id, name, description, parentId, isActive, imageFile);
        return ApiResponse.<CategoryResponse>builder()
                .result(updatedCategory)
                .message("Category updated successfully.")
                .build();
    }

    // DELETE
    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete a category", description = "Deletes a category from the database permanently.")
    public ApiResponse<String> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ApiResponse.<String>builder()
                .message("Category permanently deleted successfully.")
                .build();
    }
}