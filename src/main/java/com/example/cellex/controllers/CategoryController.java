package com.example.cellex.controllers;

import com.example.cellex.dtos.request.category.CategoryRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.CategoryResponse;
import com.example.cellex.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Category Management", description = "APIs for creating, reading, updating, and deleting product categories.")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    // CREATE - JSON Data
    @PostMapping
    @Operation(summary = "Create a new category", description = "Creates a new category using JSON data.")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) throws IOException {

        CategoryResponse newCategory = categoryService.createCategory(request, null);
        return ApiResponse.<CategoryResponse>builder()
                .result(newCategory)
                .message("Category created successfully.")
                .build();
    }

    // CREATE - Upload Image
    @PostMapping("/{id}/upload-image")
    @Operation(summary = "Upload image for category", description = "Uploads an image for an existing category.")
    public ApiResponse<CategoryResponse> uploadCategoryImage(
            @PathVariable String id,
            @Parameter(description = "Category image file", required = true)
            @RequestParam("image") MultipartFile imageFile) throws IOException {

        CategoryResponse updatedCategory = categoryService.uploadCategoryImage(id, imageFile);
        return ApiResponse.<CategoryResponse>builder()
                .result(updatedCategory)
                .message("Category image uploaded successfully.")
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

    // UPDATE - JSON Data
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing category", description = "Updates category details using JSON data.")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request) throws IOException {

        CategoryResponse updatedCategory = categoryService.updateCategory(id, request, null);
        return ApiResponse.<CategoryResponse>builder()
                .result(updatedCategory)
                .message("Category updated successfully.")
                .build();
    }

    // UPDATE - Upload Image
    @PutMapping("/{id}/upload-image")
    @Operation(summary = "Update category image", description = "Updates the image for an existing category.")
    public ApiResponse<CategoryResponse> updateCategoryImage(
            @PathVariable String id,
            @Parameter(description = "Category image file", required = true)
            @RequestParam("image") MultipartFile imageFile) throws IOException {

        CategoryResponse updatedCategory = categoryService.uploadCategoryImage(id, imageFile);
        return ApiResponse.<CategoryResponse>builder()
                .result(updatedCategory)
                .message("Category image updated successfully.")
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