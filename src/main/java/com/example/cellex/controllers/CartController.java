package com.example.cellex.controllers;

import com.example.cellex.dtos.request.cart.AddToCartRequest;
import com.example.cellex.dtos.request.cart.RemoveFromCartRequest;
import com.example.cellex.dtos.request.cart.SetCartItemQuantityRequest;
import com.example.cellex.dtos.request.cart.UpdateCartItemQuantityRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.cart.CartResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.services.cart.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.cellex.models.user.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddToCartRequest request) {

        String userId = user != null ? user.getId() : null;
        String userEmail = user != null ? user.getEmail() : "unknown";
        log.info("User {} (id={}) adding product to cart", userEmail, userId);

        CartResponse cartResponse = cartService.addToCart(userId, request);

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
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RemoveFromCartRequest request) {

        String userId = user != null ? user.getId() : null;
        String userEmail = user != null ? user.getEmail() : "unknown";
        log.info("User {} (id={}) removing {} products from cart", userEmail, userId, request.getProductIds().size());

        CartResponse cartResponse = cartService.removeFromCart(userId, request.getProductIds());

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
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@AuthenticationPrincipal User user) {
        String userId = user != null ? user.getId() : null;
        String userEmail = user != null ? user.getEmail() : "unknown";
        log.info("User {} (id={}) clearing cart", userEmail, userId);

        CartResponse cartResponse = cartService.clearCart(userId);

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
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(@AuthenticationPrincipal User user) {
        String userId = user != null ? user.getId() : null;
        String userEmail = user != null ? user.getEmail() : "unknown";
        log.info("User {} (id={}) getting cart", userEmail, userId);

        CartResponse cartResponse = cartService.getMyCart(userId);

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
                    **Mục đích:** Admin lấy danh sách tất cả giỏ hàng
                    
                    **Lưu ý quan trọng:**
                    - Chỉ ADMIN mới có quyền truy cập
                    - Trả về danh sách giỏ hàng có phân trang
                    - Dùng để quản lý và thống kê
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PageResponse<CartResponse>>> getAllCarts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortType) {

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<CartResponse> response = cartService.getAllCarts(pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<PageResponse<CartResponse>>builder()
                        .code(1000)
                        .message("Lấy danh sách giỏ hàng thành công")
                        .result(response)
                        .build());
    }

    @PatchMapping("/update-quantity")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @Operation(
            summary = "Tăng hoặc giảm số lượng sản phẩm trong giỏ hàng",
            description = """
                    **Mục đích:** Tăng hoặc giảm số lượng của một sản phẩm trong giỏ hàng lên/xuống 1 đơn vị
                    
                    **Lưu ý quan trọng:**
                    - User ID được lấy tự động từ JWT token
                    - Action có thể là: INCREASE (tăng 1) hoặc DECREASE (giảm 1)
                    - Nếu giảm xuống 0 thì sản phẩm sẽ tự động bị xóa khỏi giỏ hàng
                    - Kiểm tra stock trước khi tăng số lượng
                    
                    **Ví dụ request body:**
                    ```json
                    {
                        "productId": "product123",
                        "action": "INCREASE"
                    }
                    ```
                    
                    hoặc
                    
                    ```json
                    {
                        "productId": "product123",
                        "action": "DECREASE"
                    }
                    ```
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItemQuantity(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {

        String userId = user != null ? user.getId() : null;
        String userEmail = user != null ? user.getEmail() : "unknown";
        boolean isIncrease = request.getAction() == UpdateCartItemQuantityRequest.QuantityAction.INCREASE;
        log.info("User {} (id={}) updating cart item quantity for product {}, action: {}",
                userEmail, userId, request.getProductId(), request.getAction());

        CartResponse cartResponse = cartService.updateCartItemQuantity(
                userId,
                request.getProductId(),
                isIncrease
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<CartResponse>builder()
                        .code(1000)
                        .message("Cập nhật số lượng sản phẩm thành công")
                        .result(cartResponse)
                        .build());
    }

    @PatchMapping("/set-quantity")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @Operation(
            summary = "Thiết lập số lượng cụ thể cho sản phẩm trong giỏ hàng",
            description = """
                    **Mục đích:** Thay đổi số lượng của một sản phẩm trong giỏ hàng thành một giá trị cụ thể
                    
                    **Lưu ý quan trọng:**
                    - User ID được lấy tự động từ JWT token
                    - Số lượng phải >= 0
                    - Nếu quantity = 0 thì sản phẩm sẽ bị xóa khỏi giỏ hàng
                    - Kiểm tra stock trước khi cập nhật
                    
                    **Ví dụ request body:**
                    ```json
                    {
                        "productId": "product123",
                        "quantity": 5
                    }
                    ```
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CartResponse>> setCartItemQuantity(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SetCartItemQuantityRequest request) {

        String userId = user != null ? user.getId() : null;
        String userEmail = user != null ? user.getEmail() : "unknown";
        log.info("User {} (id={}) setting cart item quantity for product {} to {}",
                userEmail, userId, request.getProductId(), request.getQuantity());

        CartResponse cartResponse = cartService.setCartItemQuantity(
                userId,
                request.getProductId(),
                request.getQuantity()
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<CartResponse>builder()
                        .code(1000)
                        .message("Cập nhật số lượng sản phẩm thành công")
                        .result(cartResponse)
                        .build());
    }
}
