package com.example.cellex.controllers;

import com.example.cellex.dtos.request.CategoryRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.CategoryResponse;
import com.example.cellex.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    // CREATE
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new category", description = "Creates a new category. `isActive` is true by default.")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(
            @Parameter(
                    description = "Category metadata in JSON format.", required = true,
                    examples = @ExampleObject(value = """
                        {
                          "name": "Laptops",
                          "parentId": "60d5ec49c3e2a3b4c8d3f8e1"
                        }
                        """),
                    content = @Content(schema = @Schema(implementation = CategoryRequest.class))
            )
            @RequestPart("data") CategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        CategoryResponse newCategory = categoryService.createCategory(request, imageFile);
        return ApiResponse.<CategoryResponse>builder()
                .result(newCategory)
                .message("Category created successfully.")
                .build();
    }

    // READ ALL
    @GetMapping
    @Operation(summary = "Get all active categories", description = "Retrieves a list of all active categories with nested parent objects.")
    @SecurityRequirement(name = "bearerAuth", scopes = {}) // Public API
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

    // UPDATE
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update an existing category", description = "Updates category details, including its active status.")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable String id,
            @Parameter(
                    description = "Category metadata in JSON format.", required = true,
                    examples = @ExampleObject(value = """
                        {
                           "name": "Gaming Laptops",
                           "parentId": "parent_id",
                           "isActive": true
                        }
                        """),
                    content = @Content(schema = @Schema(implementation = CategoryRequest.class))
            )
            @RequestPart("data") CategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        CategoryResponse updatedCategory = categoryService.updateCategory(id, request, imageFile);
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