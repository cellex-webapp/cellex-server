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
            @RequestPart("name") @NotBlank String name,

            @Parameter(description = "Category description")
            @RequestPart(value = "description", required = false) String description,

            @Parameter(description = "Parent category ID")
            @RequestPart(value = "parentId", required = false) String parentId,

            @Parameter(description = "Category active status")
            @RequestPart(value = "isActive", required = false) Boolean isActive,

            @Parameter(description = "Category image file")
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

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
    public ApiResponse<List<CategoryResponse>> getAllActiveCategories() {
        List<CategoryResponse> categories = categoryService.getAllActiveCategories();
        return ApiResponse.<List<CategoryResponse>>builder()
                .result(categories)
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
            @RequestPart(value = "name", required = false) String name,

            @Parameter(description = "Category description")
            @RequestPart(value = "description", required = false) String description,

            @Parameter(description = "Parent category ID")
            @RequestPart(value = "parentId", required = false) String parentId,

            @Parameter(description = "Category active status")
            @RequestPart(value = "isActive", required = false) Boolean isActive,

            @Parameter(description = "Category image file")
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

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