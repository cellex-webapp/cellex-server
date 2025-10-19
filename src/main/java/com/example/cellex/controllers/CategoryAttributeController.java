package com.example.cellex.controllers;

import com.example.cellex.dtos.request.CategoryAttributeRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.CategoryAttributeResponse;
import com.example.cellex.services.CategoryAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/categories/{categoryId}/attributes")
@RequiredArgsConstructor
@Tag(name = "Category Attributes", description = "APIs quản lý thuộc tính danh mục sản phẩm - Định nghĩa các thuộc tính riêng cho từng danh mục (VD: RAM, CPU cho laptop)")
public class CategoryAttributeController {

    private final CategoryAttributeService categoryAttributeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tạo thuộc tính mới cho danh mục",
            description = """
                    **Mục đích:** Tạo thuộc tính riêng cho danh mục sản phẩm (VD: RAM, CPU, Màn hình cho Laptop)
                    
                    **Lưu ý quan trọng:**
                    - Chỉ Admin mới có quyền tạo thuộc tính
                    - attributeKey phải là duy nhất trong danh mục
                    - Với dataType = SELECT hoặc MULTI_SELECT phải có selectOptions
                    - isRequired = true nghĩa là bắt buộc nhập khi tạo sản phẩm
                    - isHighlight = true cho phép hiển thị trên card sản phẩm
                    
                    **Các kiểu dữ liệu:**
                    - TEXT: Văn bản tự do (VD: Mô tả chi tiết)
                    - NUMBER: Số (VD: Trọng lượng, Kích thước)
                    - BOOLEAN: Đúng/Sai (VD: Có hỗ trợ 5G)
                    - SELECT: Chọn 1 giá trị (VD: Màu sắc)
                    - MULTI_SELECT: Chọn nhiều giá trị (VD: Tính năng)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo thành công",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "code": 1000,
                                        "message": "Tạo thuộc tính danh mục thành công",
                                        "result": {
                                            "id": "67112345678901234567890a",
                                            "attributeName": "Dung lượng RAM",
                                            "attributeKey": "ram_capacity",
                                            "dataType": "SELECT",
                                            "unit": "GB",
                                            "isRequired": true,
                                            "isHighlight": true,
                                            "selectOptions": ["4GB", "8GB", "16GB", "32GB"],
                                            "sortOrder": 1,
                                            "description": "Dung lượng RAM của thiết bị"
                                        }
                                    }
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<ApiResponse<CategoryAttributeResponse>> createCategoryAttribute(
            @Parameter(description = "ID của danh mục sản phẩm", example = "67112345678901234567890b")
            @PathVariable String categoryId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Thông tin thuộc tính cần tạo",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "RAM Laptop", value = """
                                            {
                                                "attributeName": "Dung lượng RAM",
                                                "attributeKey": "ram_capacity",
                                                "dataType": "SELECT",
                                                "unit": "GB",
                                                "isRequired": true,
                                                "isHighlight": true,
                                                "selectOptions": ["4GB", "8GB", "16GB", "32GB"],
                                                "sortOrder": 1,
                                                "description": "Dung lượng RAM của thiết bị"
                                            }
                                            """),
                                    @ExampleObject(name = "Màu sắc Điện thoại", value = """
                                            {
                                                "attributeName": "Màu sắc",
                                                "attributeKey": "color",
                                                "dataType": "SELECT",
                                                "isRequired": true,
                                                "isHighlight": false,
                                                "selectOptions": ["Đen", "Trắng", "Xanh", "Đỏ", "Vàng"],
                                                "sortOrder": 5,
                                                "description": "Màu sắc của sản phẩm"
                                            }
                                            """),
                                    @ExampleObject(name = "Trọng lượng", value = """
                                            {
                                                "attributeName": "Trọng lượng",
                                                "attributeKey": "weight",
                                                "dataType": "NUMBER",
                                                "unit": "gram",
                                                "isRequired": false,
                                                "isHighlight": false,
                                                "validationPattern": "^[0-9]+(\\\\.[0-9]+)?$",
                                                "sortOrder": 10,
                                                "description": "Trọng lượng của sản phẩm"
                                            }
                                            """)
                            }
                    )
            )
            @Valid @RequestBody CategoryAttributeRequest request) {

        CategoryAttributeResponse response = categoryAttributeService.createCategoryAttribute(categoryId, request);
        return ResponseEntity.ok(ApiResponse.<CategoryAttributeResponse>builder()
                .code(200)
                .message("Tạo thuộc tính danh mục thành công")
                .result(response)
                .build());
    }

    @GetMapping
    @Operation(
            summary = "Lấy danh sách tất cả thuộc tính của danh mục",
            description = """
                    **Mục đích:** Lấy tất cả thuộc tính đang hoạt động của một danh mục
                    
                    **Sử dụng khi:**
                    - Hiển thị form thêm/sửa sản phẩm
                    - Xem chi tiết cấu trúc thuộc tính danh mục
                    - Quản lý thuộc tính (Admin)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách thành công",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "code": 1000,
                                        "message": "Lấy danh sách thuộc tính thành công",
                                        "result": [
                                            {
                                                "id": "67112345678901234567890a",
                                                "attributeName": "Dung lượng RAM",
                                                "attributeKey": "ram_capacity",
                                                "dataType": "SELECT",
                                                "unit": "GB",
                                                "isRequired": true,
                                                "isHighlight": true,
                                                "selectOptions": ["4GB", "8GB", "16GB", "32GB"],
                                                "sortOrder": 1
                                            },
                                            {
                                                "id": "67112345678901234567890b",
                                                "attributeName": "Màu sắc",
                                                "attributeKey": "color",
                                                "dataType": "SELECT",
                                                "isRequired": true,
                                                "isHighlight": false,
                                                "selectOptions": ["Đen", "Trắng", "Xanh", "Đỏ"],
                                                "sortOrder": 5
                                            }
                                        ]
                                    }
                                    """)))
    })
    public ResponseEntity<ApiResponse<List<CategoryAttributeResponse>>> getCategoryAttributes(
            @Parameter(description = "ID của danh mục sản phẩm", example = "67112345678901234567890b")
            @PathVariable String categoryId) {

        List<CategoryAttributeResponse> responses = categoryAttributeService.getCategoryAttributes(categoryId);
        return ResponseEntity.ok(ApiResponse.<List<CategoryAttributeResponse>>builder()
                .code(200)
                .message("Lấy danh sách thuộc tính thành công")
                .result(responses)
                .build());
    }

    @GetMapping("/highlight")
    @Operation(
            summary = "Lấy danh sách thuộc tính nổi bật",
            description = """
                    **Mục đích:** Lấy các thuộc tính nổi bật để hiển thị trên card sản phẩm (isHighlight = true)
                    
                    **Sử dụng khi:**
                    - Hiển thị thông số quan trọng trên card sản phẩm trong danh sách
                    - Tạo preview nhanh về sản phẩm
                    - Hiển thị specs chính trong kết quả tìm kiếm
                    
                    **Ví dụ:** Trong danh mục Laptop hiển thị RAM, CPU, Card đồ họa trên card
                    """
    )
    public ResponseEntity<ApiResponse<List<CategoryAttributeResponse>>> getHighlightAttributes(
            @Parameter(description = "ID của danh mục sản phẩm", example = "67112345678901234567890b")
            @PathVariable String categoryId) {

        List<CategoryAttributeResponse> responses = categoryAttributeService.getHighlightAttributes(categoryId);
        return ResponseEntity.ok(ApiResponse.<List<CategoryAttributeResponse>>builder()
                .code(200)
                .message("Lấy danh sách thuộc tính nổi bật thành công")
                .result(responses)
                .build());
    }

    @PutMapping("/{attributeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cập nhật thuộc tính danh mục",
            description = """
                    **Mục đích:** Cập nhật thông tin thuộc tính đã tồn tại
                    
                    **Lưu ý:**
                    - Chỉ Admin mới có quyền cập nhật
                    - Không thể thay đổi attributeKey nếu đã có sản phẩm sử dụng
                    - Thay đổi dataType cần cẩn thận với dữ liệu hiện có
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CategoryAttributeResponse>> updateCategoryAttribute(
            @Parameter(description = "ID của danh mục sản phẩm", example = "67112345678901234567890b")
            @PathVariable String categoryId,
            @Parameter(description = "ID của thuộc tính cần cập nhật", example = "67112345678901234567890a")
            @PathVariable String attributeId,
            @Valid @RequestBody CategoryAttributeRequest request) {

        CategoryAttributeResponse response = categoryAttributeService.updateCategoryAttribute(attributeId, request);
        return ResponseEntity.ok(ApiResponse.<CategoryAttributeResponse>builder()
                .code(200)
                .message("Cập nhật thuộc tính thành công")
                .result(response)
                .build());
    }

    @DeleteMapping("/{attributeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xóa thuộc tính danh mục",
            description = """
                    **Mục đích:** Xóa thuộc tính khỏi danh mục (soft delete)
                    
                    **Lưu ý:**
                    - Chỉ Admin mới có quyền xóa
                    - Là soft delete - không xóa hẳn khỏi database
                    - Thuộc tính bị xóa sẽ không hiển thị trong danh sách
                    - Dữ liệu sản phẩm đã có vẫn được giữ nguyên
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy thuộc tính")
    })
    public ResponseEntity<ApiResponse<String>> deleteCategoryAttribute(
            @Parameter(description = "ID của danh mục sản phẩm", example = "67112345678901234567890b")
            @PathVariable String categoryId,
            @Parameter(description = "ID của thuộc tính cần xóa", example = "67112345678901234567890a")
            @PathVariable String attributeId) {

        categoryAttributeService.deleteCategoryAttribute(attributeId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .code(200)
                .message("Xóa thuộc tính thành công")
                .result("Thuộc tính đã được xóa")
                .build());
    }
}
