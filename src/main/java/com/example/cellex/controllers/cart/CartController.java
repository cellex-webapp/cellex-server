package com.example.cellex.controllers.cart;

import com.example.cellex.dtos.request.cart.AddToCartRequest;
import com.example.cellex.dtos.request.cart.RemoveFromCartRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.cart.CartResponse;
import com.example.cellex.services.cart.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "13. Shopping Cart", description = "APIs quản lý giỏ hàng")
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @Operation(
            summary = "Thêm sản phẩm vào giỏ hàng",
            description = """
                    **Mục đích:** Thêm sản phẩm vào giỏ hàng của người dùng hiện tại
                    
                    **Lưu ý quan trọng:**
                    - User ID được lấy tự động từ JWT token
                    - Nếu sản phẩm đã có trong giỏ hàng, số lượng sẽ được cộng dồn
                    - Kiểm tra stock trước khi thêm
                    - Tự động tạo giỏ hàng mới nếu user chưa có
                    
                    **Ví dụ request body:**
                    ```json
                    {
                        "productId": "product123",
                        "quantity": 2
                    }
                    ```
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request) {

        String userEmail = authentication.getName();
        log.info("User {} adding product to cart", userEmail);

        CartResponse cartResponse = cartService.addToCart(userEmail, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<CartResponse>builder()
                        .code(1000)
                        .message("Thêm sản phẩm vào giỏ hàng thành công")
                        .result(cartResponse)
                        .build());
    }

    @DeleteMapping("/remove")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @Operation(
            summary = "Xóa sản phẩm khỏi giỏ hàng",
            description = """
                    **Mục đích:** Xóa một hoặc nhiều sản phẩm khỏi giỏ hàng
                    
                    **Lưu ý quan trọng:**
                    - Hỗ trợ xóa nhiều sản phẩm cùng lúc
                    - User ID được lấy tự động từ JWT token
                    - Tự động cập nhật lại tổng giá và số lượng
                    
                    **Ví dụ request body:**
                    ```json
                    {
                        "productIds": ["product123", "product456"]
                    }
                    ```
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            Authentication authentication,
            @Valid @RequestBody RemoveFromCartRequest request) {

        String userEmail = authentication.getName();
        log.info("User {} removing {} products from cart", userEmail, request.getProductIds().size());

        CartResponse cartResponse = cartService.removeFromCart(userEmail, request.getProductIds());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<CartResponse>builder()
                        .code(1000)
                        .message("Xóa sản phẩm khỏi giỏ hàng thành công")
                        .result(cartResponse)
                        .build());
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @Operation(
            summary = "Xóa tất cả sản phẩm khỏi giỏ hàng",
            description = """
                    **Mục đích:** Xóa toàn bộ sản phẩm trong giỏ hàng của người dùng
                    
                    **Lưu ý quan trọng:**
                    - Xóa tất cả items nhưng giữ lại giỏ hàng
                    - User ID được lấy tự động từ JWT token
                    - Reset tổng giá và số lượng về 0
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("User {} clearing cart", userEmail);

        CartResponse cartResponse = cartService.clearCart(userEmail);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<CartResponse>builder()
                        .code(1000)
                        .message("Xóa tất cả sản phẩm khỏi giỏ hàng thành công")
                        .result(cartResponse)
                        .build());
    }

    @GetMapping("/my-cart")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @Operation(
            summary = "Lấy giỏ hàng của tôi",
            description = """
                    **Mục đích:** Lấy thông tin giỏ hàng của người dùng hiện tại
                    
                    **Lưu ý quan trọng:**
                    - User ID được lấy tự động từ JWT token
                    - Tự động tạo giỏ hàng mới nếu user chưa có
                    - Hiển thị thông tin stock hiện tại của từng sản phẩm
                    - Hiển thị trạng thái availability của sản phẩm
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("User {} getting cart", userEmail);

        CartResponse cartResponse = cartService.getMyCart(userEmail);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<CartResponse>builder()
                        .code(1000)
                        .message("Lấy giỏ hàng thành công")
                        .result(cartResponse)
                        .build());
    }

    @GetMapping("/{cartId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy giỏ hàng theo ID",
            description = """
                    **Mục đích:** Admin lấy thông tin giỏ hàng theo ID
                    
                    **Lưu ý quan trọng:**
                    - Chỉ ADMIN mới có quyền truy cập
                    - Dùng để quản lý và hỗ trợ khách hàng
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CartResponse>> getCartById(@PathVariable String cartId) {
        log.info("Admin getting cart by id: {}", cartId);

        CartResponse cartResponse = cartService.getCartById(cartId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<CartResponse>builder()
                        .code(1000)
                        .message("Lấy giỏ hàng thành công")
                        .result(cartResponse)
                        .build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy tất cả giỏ hàng",
            description = """
                    **Mục đích:** Admin lấy danh sách tất cả giỏ hàng với phân trang
                    
                    **Lưu ý quan trọng:**
                    - Chỉ ADMIN mới có quyền truy cập
                    - Hỗ trợ phân trang với page, size
                    - Dùng để quản lý và thống kê
                    
                    **Ví dụ:**
                    - GET /api/v1/carts?page=0&size=10
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Page<CartResponse>>> getAllCarts(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Admin getting all carts with pagination");

        Page<CartResponse> carts = cartService.getAllCarts(pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<Page<CartResponse>>builder()
                        .code(1000)
                        .message("Lấy danh sách giỏ hàng thành công")
                        .result(carts)
                        .build());
    }
}

