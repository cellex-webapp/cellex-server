package com.example.cellex.controllers;

import com.example.cellex.dtos.request.CategoryRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.models.Category;
import com.example.cellex.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
    @Operation(summary = "Create a new category", description = "Creates a new category with a name and optional parent/image. Requires ADMIN role.")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Category> createCategory(
            // CHÍNH DÒNG NÀY SẼ GIÚP HIỂN THỊ CHI TIẾT
            @Parameter(
                    description = "Category details in JSON format. This part represents the metadata of the category.",
                    required = true,
                    schema = @Schema(implementation = CategoryRequest.class),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CategoryRequest.class))
            )
            @RequestPart("data") CategoryRequest request,

            @Parameter(description = "Optional image file for the category", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        Category newCategory = categoryService.createCategory(request, imageFile);
        return ApiResponse.<Category>builder()
                .result(newCategory)
                .message("Category created successfully.")
                .build();
    }

    // READ ALL
    @GetMapping
    @Operation(summary = "Get all active categories", description = "Retrieves a list of all categories marked as active. This endpoint is public.")
    @SecurityRequirement(name = "bearerAuth", scopes = {})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved categories")
    })
    public ApiResponse<List<Category>> getAllActiveCategories() {
        List<Category> categories = categoryService.getAllActiveCategories();
        return ApiResponse.<List<Category>>builder()
                .result(categories)
                .build();
    }

    // READ ONE
    @GetMapping("/{id}")
    @Operation(summary = "Get a category by ID", description = "Retrieves a single category by its unique ID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved category"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ApiResponse<Category> getCategoryById(@PathVariable String id) {
        Category category = categoryService.getCategoryById(id);
        return ApiResponse.<Category>builder()
                .result(category)
                .build();
    }

    // UPDATE
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update an existing category", description = "Updates the details of an existing category. Requires ADMIN role.")
    public ApiResponse<Category> updateCategory(
            @PathVariable String id,

            @Parameter(description = "Category details in JSON format",
                    schema = @Schema(implementation = CategoryRequest.class),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @RequestPart("data") CategoryRequest request,

            @Parameter(description = "Optional new image file to update")
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        Category updatedCategory = categoryService.updateCategory(id, request, imageFile);
        return ApiResponse.<Category>builder()
                .result(updatedCategory)
                .message("Category updated successfully.")
                .build();
    }

    // DELETE
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category (Soft delete)", description = "Marks a category as inactive instead of deleting it from the database. Requires ADMIN role.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden, requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ApiResponse<String> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ApiResponse.<String>builder()
                .message("Category deleted successfully.")
                .build();
    }
}