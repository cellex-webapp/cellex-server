package com.example.cellex.controllers;

import com.example.cellex.dtos.request.ProductRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.ProductResponse;
import com.example.cellex.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "APIs quản lý sản phẩm")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "Tạo sản phẩm mới",
            description = "Vendor tạo sản phẩm mới cho cửa hàng của mình",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            Authentication authentication,
            @Valid @RequestBody ProductRequest request) {

        String vendorId = authentication.getName();
        ProductResponse response = productService.createProduct(vendorId, request);

        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .code(1000)
                .message("Tạo sản phẩm thành công")
                .result(response)
                .build());
    }

    @GetMapping("/{productId}")
    @Operation(
            summary = "Lấy thông tin sản phẩm theo ID",
            description = "Lấy thông tin chi tiết sản phẩm bằng ID"
    )
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "ID của sản phẩm") @PathVariable String productId) {

        ProductResponse response = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .code(1000)
                .message("Lấy thông tin sản phẩm thành công")
                .result(response)
                .build());
    }

    @GetMapping("/category/{categoryId}")
    @Operation(
            summary = "Lấy sản phẩm theo danh mục",
            description = "Lấy danh sách sản phẩm đã xuất bản trong một danh mục"
    )
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByCategory(
            @Parameter(description = "ID của danh mục") @PathVariable String categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ProductResponse> response = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.<Page<ProductResponse>>builder()
                .code(1000)
                .message("Lấy sản phẩm theo danh mục thành công")
                .result(response)
                .build());
    }

    @GetMapping("/shop/{shopId}")
    @Operation(
            summary = "Lấy sản phẩm theo cửa hàng",
            description = "Lấy danh sách sản phẩm đã xuất bản của một cửa hàng"
    )
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByShop(
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ProductResponse> response = productService.getProductsByShop(shopId, pageable);
        return ResponseEntity.ok(ApiResponse.<Page<ProductResponse>>builder()
                .code(1000)
                .message("Lấy sản phẩm theo cửa hàng thành công")
                .result(response)
                .build());
    }

    @GetMapping("/search")
    @Operation(
            summary = "Tìm kiếm sản phẩm",
            description = "Tìm kiếm sản phẩm theo từ khóa trong tên"
    )
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ProductResponse> response = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.<Page<ProductResponse>>builder()
                .code(1000)
                .message("Tìm kiếm sản phẩm thành công")
                .result(response)
                .build());
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "Cập nhật sản phẩm",
            description = "Vendor cập nhật thông tin sản phẩm của mình",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            Authentication authentication,
            @Parameter(description = "ID của sản phẩm") @PathVariable String productId,
            @Valid @RequestBody ProductRequest request) {

        String vendorId = authentication.getName();
        ProductResponse response = productService.updateProduct(vendorId, productId, request);

        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .code(1000)
                .message("Cập nhật sản phẩm thành công")
                .result(response)
                .build());
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "Xóa sản phẩm",
            description = "Vendor xóa sản phẩm của mình",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            Authentication authentication,
            @Parameter(description = "ID của sản phẩm") @PathVariable String productId) {

        String vendorId = authentication.getName();
        productService.deleteProduct(vendorId, productId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .code(1000)
                .message("Xóa sản phẩm thành công")
                .result("Sản phẩm đã được xóa")
                .build());
    }

    @PatchMapping("/{productId}/publish")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "Chuyển đổi trạng thái xuất bản",
            description = "Vendor bật/tắt trạng thái xuất bản sản phẩm",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<ProductResponse>> togglePublishStatus(
            Authentication authentication,
            @Parameter(description = "ID của sản phẩm") @PathVariable String productId) {

        String vendorId = authentication.getName();
        ProductResponse response = productService.togglePublishStatus(vendorId, productId);

        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .code(1000)
                .message("Thay đổi trạng thái xuất bản thành công")
                .result(response)
                .build());
    }
}
