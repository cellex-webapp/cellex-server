package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.product.ProductComparisonResponse;
import com.example.cellex.dtos.response.product.ProductResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.services.product.ProductComparisonService;
import com.example.cellex.services.product.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "11. Products", description = "APIs quản lý sản phẩm")
public class ProductController {

    private final ProductService productService;
    private final ProductComparisonService productComparisonService;

    // CREATE - Multipart Form Data
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "Tạo sản phẩm mới",
            description = """
                    **Mục đích:** Vendor tạo sản phẩm mới cho cửa hàng của mình sử dụng multipart form data với upload ảnh
                    
                    **Lưu ý quan trọng:**
                    - Chỉ VENDOR đã được verify mới có quyền tạo sản phẩm
                    - Sử dụng Content-Type: multipart/form-data
                    - Có thể upload nhiều ảnh cùng lúc hoặc không upload ảnh nào
                    - attributeValues là JSON string chứa thông tin các thuộc tính sản phẩm
                    - finalPrice sẽ được tính tự động từ price và saleOff
                    
                    **Format attributeValues (JSON string):**
                    ```json
                    [
                        {"attributeId": "attr1_id", "value": "Màu đỏ"},
                        {"attributeId": "attr2_id", "value": "32GB"},
                        {"attributeId": "attr3_id", "value": "1920x1080"}
                    ]
                    ```
                    
                    **Ví dụ tạo sản phẩm iPhone:**
                    - categoryId: "smartphone_category_id"
                    - name: "iPhone 15 Pro Max"
                    - description: "iPhone 15 Pro Max với chip A17 Pro mạnh mẽ"
                    - price: "29990000"
                    - saleOff: "5" (giảm 5%)
                    - stockQuantity: "50"
                    - isPublished: "true"
                    - attributeValues: JSON string chứa màu sắc, bộ nhớ, v.v.
                    - images: Các file ảnh sản phẩm
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Tạo sản phẩm thành công",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                                "code": 1000,
                                                "message": "Tạo sản phẩm thành công",
                                                "result": {
                                                    "id": "product123",
                                                    "shopId": "shop123",
                                                    "categoryId": "smartphone_category",
                                                    "name": "iPhone 15 Pro Max",
                                                    "description": "iPhone 15 Pro Max với chip A17 Pro mạnh mẽ",
                                                    "images": [
                                                        "https://cloudinary.com/image1.jpg",
                                                        "https://cloudinary.com/image2.jpg"
                                                    ],
                                                    "price": 29990000.0,
                                                    "saleOff": 5.0,
                                                    "finalPrice": 28490500.0,
                                                    "stockQuantity": 50,
                                                    "attributeValues": [
                                                        {
                                                            "attributeId": "color_attr",
                                                            "attributeKey": "color",
                                                            "attributeName": "Màu sắc",
                                                            "value": "Titan Tự Nhiên",
                                                            "unit": null,
                                                            "dataType": "SELECT"
                                                        },
                                                        {
                                                            "attributeId": "storage_attr", 
                                                            "attributeKey": "storage",
                                                            "attributeName": "Bộ nhớ trong",
                                                            "value": "256GB",
                                                            "unit": "GB",
                                                            "dataType": "SELECT"
                                                        }
                                                    ],
                                                    "averageRating": 0.0,
                                                    "reviewCount": 0,
                                                    "purchaseCount": 0,
                                                    "isPublished": true,
                                                    "createdAt": "2024-01-15T10:30:00",
                                                    "updatedAt": "2024-01-15T10:30:00"
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền - chỉ VENDOR được verify")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            Authentication authentication,

            @Parameter(
                    description = "ID danh mục sản phẩm",
                    required = true,
                    example = "smartphone_category_id"
            )
            @RequestParam("categoryId") @NotBlank String categoryId,

            @Parameter(
                    description = "Tên sản phẩm",
                    required = true,
                    example = "iPhone 15 Pro Max 256GB"
            )
            @RequestParam("name") @NotBlank String name,

            @Parameter(
                    description = "Mô tả chi tiết sản phẩm",
                    example = "iPhone 15 Pro Max với chip A17 Pro mạnh mẽ, camera 48MP, màn hình Super Retina XDR 6.7 inch"
            )
            @RequestParam(value = "description", required = false) String description,

            @Parameter(
                    description = "Giá sản phẩm (VND)",
                    required = true,
                    example = "29990000"
            )
            @RequestParam("price") @NotNull @DecimalMin("0.0") String price,

            @Parameter(
                    description = "Phần trăm giảm giá (0-100)",
                    example = "5"
            )
            @RequestParam(value = "saleOff", required = false) String saleOff,

            @Parameter(
                    description = "Số lượng tồn kho",
                    required = true,
                    example = "50"
            )
            @RequestParam("stockQuantity") @NotNull @Min(0) String stockQuantity,

            @Parameter(
                    description = """
                            Thuộc tính sản phẩm (JSON format)
                            
                            **Ví dụ:**
                            ```json
                            [
                                {"attributeId": "67112345678901234567890a", "value": "Titan Tự Nhiên"},
                                {"attributeId": "67112345678901234567890b", "value": "256GB"},
                                {"attributeId": 67112345678901234567890d", "value": "8GB"},
                                {"attributeId": "67112345678901234567890c", "value": "6.7 inch"}
                            ]
                            ```
                            """,
                    example = """
                            [
                                {"attributeId": "67112345678901234567890a", "value": "Titan Tự Nhiên"},
                                {"attributeId": "67112345678901234567890b", "value": "256GB"},
                                {"attributeId": "67112345678901234567890c", "value": "8GB"}
                            ]
                            """
            )
            @RequestParam(value = "attributeValues", required = false) String attributeValues,

            @Parameter(
                    description = "Trạng thái xuất bản (true/false)",
                    example = "true"
            )
            @RequestParam(value = "isPublished", required = false) String isPublished,

            @Parameter(
                    description = "Ảnh sản phẩm (có thể upload nhiều file)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
                    )
            )
            @RequestPart(value = "images", required = false) MultipartFile[] images) throws IOException {

        String vendorId = ((User)authentication.getPrincipal()).getId();
        ProductResponse response = productService.createProductMultipart(
                vendorId, categoryId, name, description, price, saleOff,
                stockQuantity, attributeValues, isPublished, images);

        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .code(1000)
                .message("Tạo sản phẩm thành công")
                .result(response)
                .build());
    }

    @GetMapping("/compare")
    @Operation(
            summary = "So sánh sản phẩm",
            description = """
                    **Mục đích:** So sánh thông số kỹ thuật giữa các sản phẩm cùng danh mục.
                    
                    **Lưu ý:**
                    - Tối thiểu 2 và tối đa 4 sản phẩm
                    - Tất cả sản phẩm phải thuộc cùng một danh mục
                    - Response bao gồm: thông tin cơ bản, bảng so sánh thông số, đánh dấu khác biệt, best-in-class
                    """
    )
    public ResponseEntity<ApiResponse<ProductComparisonResponse>> compareProducts(
            @Parameter(description = "Danh sách ID sản phẩm cần so sánh (2-4 ID, phân tách bằng dấu phẩy)")
            @RequestParam List<String> ids) {

        ProductComparisonResponse response = productComparisonService.getComparison(ids);
        return ResponseEntity.ok(ApiResponse.<ProductComparisonResponse>builder()
                .code(1000)
                .message("So sánh sản phẩm thành công")
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
            description = "Lấy danh sách sản phẩm đã xuất bản trong một danh mục với phân trang. Có thể dùng slug hoặc ID của category"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByCategory(
            @Parameter(description = "Slug hoặc ID của danh mục") @PathVariable String categoryId,

            @Parameter(description = "Số trang (bắt đầu từ 1)")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "Số lượng sản phẩm mỗi trang")
            @RequestParam(defaultValue = "20") Integer limit,

            @Parameter(description = "Kiểu sắp xếp (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Trường để sắp xếp")
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<ProductResponse> response = productService.getProductsByCategorySlugOrId(categoryId, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ProductResponse>>builder()
                .code(1000)
                .message("Lấy sản phẩm theo danh mục thành công")
                .result(response)
                .build());
    }

    @GetMapping("/shop/{shopId}")
    @Operation(
            summary = "Lấy sản phẩm theo cửa hàng",
            description = "Lấy danh sách sản phẩm đã xuất bản của một cửa hàng với phân trang"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByShop(
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,

            @Parameter(description = "Số trang (bắt đầu từ 1)")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "Số lượng sản phẩm mỗi trang")
            @RequestParam(defaultValue = "20") Integer limit,

            @Parameter(description = "Kiểu sắp xếp (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Trường để sắp xếp")
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<ProductResponse> response = productService.getProductsByShop(shopId, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ProductResponse>>builder()
                .code(1000)
                .message("Lấy sản phẩm theo cửa hàng thành công")
                .result(response)
                .build());
    }

    @GetMapping("/search")
    @Operation(
            summary = "Tìm kiếm sản phẩm",
            description = "Tìm kiếm sản phẩm theo từ khóa trong tên với phân trang"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @Parameter(description = "Từ khóa tìm kiếm")
            @RequestParam String keyword,

            @Parameter(description = "Số trang (bắt đầu từ 1)")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "Số lượng sản phẩm mỗi trang")
            @RequestParam(defaultValue = "15") Integer limit,

            @Parameter(description = "Kiểu sắp xếp (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Trường để sắp xếp")
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<ProductResponse> response = productService.searchProducts(keyword, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ProductResponse>>builder()
                .code(1000)
                .message("Tìm kiếm sản phẩm thành công")
                .result(response)
                .build());
    }

    @GetMapping
    @Operation(
            summary = "Lấy tất cả sản phẩm",
            description = "Lấy danh sách tất cả sản phẩm với phân trang"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Số trang (bắt đầu từ 1)")
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "Số lượng sản phẩm mỗi trang")
            @RequestParam(defaultValue = "15") Integer limit,
            
            @Parameter(description = "Kiểu sắp xếp (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType,
            
            @Parameter(description = "Trường để sắp xếp")
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<ProductResponse> response = productService.getAllProducts(pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ProductResponse>>builder()
                .code(1000)
                .message("Lấy danh sách sản phẩm thành công")
                .result(response)
                .build());
    }

    // GET MY PRODUCTS - Vendor xem tất cả sản phẩm của mình
    @GetMapping("/my-products")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "Lấy tất cả sản phẩm của vendor",
            description = """
                    **Mục đích:** Vendor xem tất cả sản phẩm của mình, bao gồm cả đã xuất bản và chưa xuất bản
                    
                    **Lưu ý:**
                    - Chỉ VENDOR mới có quyền truy cập endpoint này
                    - Trả về tất cả sản phẩm của shop thuộc vendor (published và unpublished)
                    - Hỗ trợ phân trang và sắp xếp
                    - Vendor ID được lấy từ JWT token
                    
                    **Use case:**
                    - Vendor quản lý tất cả sản phẩm của mình
                    - Xem sản phẩm đã xuất bản và chưa xuất bản
                    - Chỉnh sửa hoặc xóa sản phẩm
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Lấy danh sách sản phẩm thành công"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Chưa đăng nhập"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Không có quyền - chỉ VENDOR"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Không tìm thấy shop của vendor"
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getMyProducts(
            Authentication authentication,

            @Parameter(description = "Số trang (bắt đầu từ 1)")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "Số lượng sản phẩm mỗi trang")
            @RequestParam(defaultValue = "20") Integer limit,

            @Parameter(description = "Kiểu sắp xếp (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Trường để sắp xếp (createdAt, name, price, stockQuantity, isPublished)")
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        String vendorId = ((User) authentication.getPrincipal()).getId();

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<ProductResponse> response = productService.getMyProducts(vendorId, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ProductResponse>>builder()
                .code(1000)
                .message("Lấy danh sách sản phẩm của vendor thành công")
                .result(response)
                .build());
    }

    // UPDATE - Multipart Form Data
    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "Cập nhật sản phẩm",
            description = """
                    **Mục đích:** Vendor cập nhật thông tin sản phẩm của mình sử dụng multipart form data với upload ảnh
                    
                    **Lưu ý quan trọng:**
                    - Chỉ có thể cập nhật sản phẩm của chính mình
                    - Tất cả các trường đều là tùy chọn - chỉ cập nhật những trường được gửi
                    - Nếu upload ảnh mới sẽ thay thế toàn bộ ảnh cũ
                    - finalPrice sẽ được tính lại tự động khi thay đổi price hoặc saleOff
                    
                    **Ví dụ cập nhật sản phẩm:**
                    - Chỉ cập nhật giá: gửi price = "27990000"
                    - Chỉ cập nhật mô tả: gửi description = "Mô tả mới"
                    - Cập nhật ảnh: upload file mới vào images
                    - Cập nhật thuộc tính: gửi attributeValues với JSON mới
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cập nhật sản phẩm thành công",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                                "code": 1000,
                                                "message": "Cập nhật sản phẩm thành công",
                                                "result": {
                                                    "id": "product123",
                                                    "name": "iPhone 15 Pro Max 512GB",
                                                    "price": 34990000.0,
                                                    "saleOff": 10.0,
                                                    "finalPrice": 31491000.0,
                                                    "stockQuantity": 30,
                                                    "attributeValues": [
                                                        {
                                                            "attributeId": "storage_attr",
                                                            "attributeName": "Bộ nhớ trong", 
                                                            "value": "512GB",
                                                            "unit": "GB"
                                                        }
                                                    ],
                                                    "images": [
                                                        "https://cloudinary.com/new_image1.jpg",
                                                        "https://cloudinary.com/new_image2.jpg",
                                                        "https://cloudinary.com/new_image3.jpg"
                                                    ],
                                                    "updatedAt": "2024-01-15T14:25:00"
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền - chỉ được sửa sản phẩm của mình"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            Authentication authentication,

            @Parameter(
                    description = "ID của sản phẩm cần cập nhật",
                    example = "product123"
            )
            @PathVariable String productId,

            @Parameter(
                    description = "ID danh mục mới (nếu muốn chuyển danh mục)",
                    example = "laptop_category_id"
            )
            @RequestParam(value = "categoryId", required = false) String categoryId,

            @Parameter(
                    description = "Tên sản phẩm mới",
                    example = "iPhone 15 Pro Max 512GB - Phiên bản nâng cấp"
            )
            @RequestParam(value = "name", required = false) String name,

            @Parameter(
                    description = "Mô tả sản phẩm mới",
                    example = "iPhone 15 Pro Max 512GB với nhiều cải tiến về camera và hiệu năng"
            )
            @RequestParam(value = "description", required = false) String description,

            @Parameter(
                    description = "Giá mới (VND)",
                    example = "34990000"
            )
            @RequestParam(value = "price", required = false) String price,

            @Parameter(
                    description = "Phần trăm giảm giá mới (0-100)",
                    example = "10"
            )
            @RequestParam(value = "saleOff", required = false) String saleOff,

            @Parameter(
                    description = "Số lượng tồn kho mới",
                    example = "30"
            )
            @RequestParam(value = "stockQuantity", required = false) String stockQuantity,

            @Parameter(
                    description = """
                            Thuộc tính sản phẩm mới (JSON format) - sẽ thay thế toàn bộ thuộc tính cũ
                            
                            **Ví dụ cập nhật bộ nhớ và màu sắc:**
                            ```json
                            [
                                {"attributeId": "color_attr_id", "value": "Titan Xanh"},
                                {"attributeId": "storage_attr_id", "value": "512GB"},
                                {"attributeId": "ram_attr_id", "value": "8GB"}
                            ]
                            ```
                            """,
                    example = """
                            [
                                {"attributeId": "67112345678901234567890a", "value": "Titan Xanh"},
                                {"attributeId": "67112345678901234567890b", "value": "512GB"}
                            ]
                            """
            )
            @RequestParam(value = "attributeValues", required = false) String attributeValues,

            @Parameter(
                    description = "Trạng thái xuất bản mới",
                    example = "false"
            )
            @RequestParam(value = "isPublished", required = false) String isPublished,

            @Parameter(
                    description = "Ảnh sản phẩm mới (sẽ thay thế toàn bộ ảnh cũ)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
                    )
            )
            @RequestPart(value = "images", required = false) MultipartFile[] images) throws IOException {

        String vendorId = ((User)authentication.getPrincipal()).getId();
        ProductResponse response = productService.updateProductMultipart(
                vendorId, productId, categoryId, name, description, price, saleOff,
                stockQuantity, attributeValues, isPublished, images);

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

        String vendorId = ((User)authentication.getPrincipal()).getId();
        productService.deleteProduct(vendorId, productId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .code(1000)
                .message("Xóa sản phẩm thành công")
                .build());
    }
}
