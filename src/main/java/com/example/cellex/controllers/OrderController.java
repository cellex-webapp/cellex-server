package com.example.cellex.controllers;

import com.example.cellex.dtos.request.order.*;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.order.AvailableCouponResponse;
import com.example.cellex.dtos.response.order.OrderResponse;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.models.user.User;
import com.example.cellex.services.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "13. Orders", description = "APIs quản lý đơn hàng")
public class OrderController {

    private final OrderService orderService;

    // ==================== USER APIS ====================

    @PostMapping("/from-product")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Tạo đơn hàng từ trang sản phẩm",
            description = "Tạo đơn hàng trực tiếp từ trang chi tiết sản phẩm",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<OrderResponse>> createOrderFromProduct(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {

        String userId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.createOrderFromProduct(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<OrderResponse>builder()
                        .code(1000)
                        .message("Tạo đơn hàng từ sản phẩm thành công")
                        .result(response)
                        .build());
    }

    @PostMapping("/from-cart")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Tạo đơn hàng từ giỏ hàng",
            description = "Tạo đơn hàng từ các sản phẩm đã chọn trong giỏ hàng",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<OrderResponse>> createOrderFromCart(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {

        String userId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.createOrderFromCart(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<OrderResponse>builder()
                        .code(1000)
                        .message("Tạo đơn hàng từ giỏ hàng thành công")
                        .result(response)
                        .build());
    }

    @GetMapping("/{orderId}/coupons/available")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Lấy danh sách mã giảm giá khả dụng",
            description = """
                    **Mục đích:** Hiển thị danh sách mã giảm giá có thể áp dụng cho đơn hàng
                    
                    **Lưu ý:**
                    - Chỉ hiển thị các mã đang ACTIVE
                    - Kiểm tra điều kiện tối thiểu của đơn hàng
                    - Kiểm tra sản phẩm/danh mục áp dụng
                    - Sắp xếp theo khả năng áp dụng và giá trị giảm giá
                    - Hiển thị preview số tiền sẽ được giảm
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<AvailableCouponResponse>>> getAvailableCoupons(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId) {

        String userId = ((User) authentication.getPrincipal()).getId();
        List<AvailableCouponResponse> response = orderService.getAvailableCoupons(userId, orderId);

        return ResponseEntity.ok(ApiResponse.<List<AvailableCouponResponse>>builder()
                .code(1000)
                .message("Lấy danh sách mã giảm giá thành công")
                .result(response)
                .build());
    }

    @PostMapping("/{orderId}/coupons/apply")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Áp dụng mã giảm giá",
            description = """
                    **Mục đích:** Áp dụng mã giảm giá cho đơn hàng
                    
                    **Lưu ý:**
                    - Chỉ áp dụng được khi đơn hàng ở trạng thái PENDING
                    - Kiểm tra mã có thuộc về user không
                    - Kiểm tra mã còn hiệu lực không
                    - Kiểm tra điều kiện áp dụng
                    - Tính toán và cập nhật tổng tiền đơn hàng
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> applyCoupon(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId,
            @Valid @RequestBody ApplyCouponRequest request) {

        String userId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.applyCoupon(userId, orderId, request);

        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Áp dụng mã giảm giá thành công")
                .result(response)
                .build());
    }

    @DeleteMapping("/{orderId}/coupons")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Bỏ mã giảm giá",
            description = """
                    **Mục đích:** Hủy áp dụng mã giảm giá cho đơn hàng
                    
                    **Lưu ý:**
                    - Chỉ bỏ được khi đơn hàng ở trạng thái PENDING
                    - Tính toán lại tổng tiền đơn hàng
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> removeCoupon(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId) {

        String userId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.removeCoupon(userId, orderId);

        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Bỏ mã giảm giá thành công")
                .result(response)
                .build());
    }

    @PostMapping("/{orderId}/checkout")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Xác nhận đặt hàng (Checkout)",
            description = """
                    **Mục đích:** Xác nhận đặt hàng - bước cuối cùng của quá trình đặt hàng
                    
                    **Luồng hoạt động:**
                    1. Tự động lấy địa chỉ từ thông tin user
                    2. User chọn phương thức thanh toán (hiện tại chỉ có COD)
                    3. Xác nhận đặt hàng
                    
                    **Xử lý khi checkout:**
                    - Trừ số lượng tồn kho của sản phẩm
                    - Đánh dấu coupon đã sử dụng (nếu có)
                    - Xóa sản phẩm khỏi giỏ hàng (nếu đặt từ giỏ hàng)
                    - Giữ nguyên trạng thái PENDING, chờ vendor xác nhận
                    
                    **Lưu ý:**
                    - Địa chỉ giao hàng lấy từ profile của user
                    - User cần cập nhật địa chỉ trong profile trước khi đặt hàng
                    - Kiểm tra lại stock trước khi trừ
                    - Sau bước này không thể thay đổi sản phẩm hoặc coupon
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> checkoutOrder(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId,
            @Valid @RequestBody CheckoutOrderRequest request) {

        String userId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.checkoutOrder(userId, orderId, request);

        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Đặt hàng thành công")
                .result(response)
                .build());
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Hủy đơn hàng",
            description = """
                    **Mục đích:** User hủy đơn hàng
                    
                    **Điều kiện:**
                    - Chỉ hủy được khi đơn hàng ở trạng thái PENDING (chờ xác nhận)
                    - Không thể hủy sau khi vendor đã xác nhận
                    
                    **Xử lý khi hủy:**
                    - Hoàn lại số lượng tồn kho
                    - Hoàn lại coupon (nếu có)
                    - Cập nhật trạng thái thành CANCELLED
                    - Lưu lý do hủy mặc định từ server
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId) {

        String userId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.cancelOrder(userId, orderId);

        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Hủy đơn hàng thành công")
                .result(response)
                .build());
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Xóa đơn hàng",
            description = """
                    **Mục đích:** User xóa đơn hàng
                    
                    **Điều kiện:**
                    - Chỉ xóa được khi đơn hàng ở trạng thái PENDING hoặc CANCELLED
                    - Không thể xóa sau khi đã xác nhận
                    
                    **Xử lý khi xóa:**
                    - Hoàn lại số lượng tồn kho (nếu đơn hàng chưa bị hủy)
                    - Hoàn lại coupon (nếu có và đơn hàng chưa bị hủy)
                    - Hoàn lại sản phẩm vào giỏ hàng (nếu đơn hàng được tạo từ giỏ hàng)
                    - Xóa đơn hàng khỏi database
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId) {

        String userId = ((User) authentication.getPrincipal()).getId();
        orderService.deleteOrder(userId, orderId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa đơn hàng thành công")
                .build());
    }

    @PostMapping("/{orderId}/confirm-delivery")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Xác nhận đã nhận hàng",
            description = """
                    **Mục đích:** User xác nhận đã nhận được hàng
                    
                    **Điều kiện:**
                    - Chỉ xác nhận được khi đơn hàng ở trạng thái SHIPPING (đang vận chuyển)
                    
                    **Xử lý:**
                    - Cập nhật trạng thái thành DELIVERED
                    - Lưu thời gian giao hàng thành công
                    - Thêm vào lịch sử trạng thái
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> confirmDelivery(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId) {

        String userId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.confirmDelivery(userId, orderId);

        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Xác nhận đã nhận hàng thành công")
                .result(response)
                .build());
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Lấy danh sách đơn hàng của tôi",
            description = """
                    **Mục đích:** User xem tất cả đơn hàng của mình
                    
                    **Hỗ trợ:**
                    - Phân trang
                    - Sắp xếp theo thời gian tạo (mới nhất trước)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            Authentication authentication,
            @Parameter(description = "Số trang (bắt đầu từ 1)")
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "Số lượng đơn hàng mỗi trang")
            @RequestParam(defaultValue = "10") Integer limit,
            
            @Parameter(description = "Sắp xếp theo (createdAt, totalAmount, status)")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Kiểu sắp xếp (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType) {

        String userId = ((User) authentication.getPrincipal()).getId();
        
        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<OrderResponse> response = orderService.getMyOrders(userId, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<OrderResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đơn hàng thành công")
                .result(response)
                .build());
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Lấy chi tiết đơn hàng",
            description = "Xem thông tin chi tiết một đơn hàng",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId) {

        String userId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.getOrderById(userId, orderId);

        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Lấy thông tin đơn hàng thành công")
                .result(response)
                .build());
    }

    // ==================== VENDOR APIS ====================

    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "[VENDOR] Xác nhận đơn hàng",
            description = """
                    **Mục đích:** Vendor xác nhận đơn hàng sau khi kiểm tra
                    
                    **Điều kiện:**
                    - Chỉ xác nhận được đơn hàng ở trạng thái PENDING
                    - Đơn hàng phải thuộc shop của vendor
                    
                    **Xử lý:**
                    - Cập nhật trạng thái thành CONFIRMED
                    - Lưu thời gian xác nhận
                    - Sau bước này user không thể hủy đơn
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId) {

        String vendorId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.confirmOrder(vendorId, orderId);

        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Xác nhận đơn hàng thành công")
                .result(response)
                .build());
    }

    @PostMapping("/{orderId}/ship")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "[VENDOR] Xác nhận đã gửi hàng",
            description = """
                    **Mục đích:** Vendor xác nhận đã giao hàng cho đơn vị vận chuyển
                    
                    **Điều kiện:**
                    - Chỉ xác nhận được đơn hàng ở trạng thái CONFIRMED
                    - Đơn hàng phải thuộc shop của vendor
                    
                    **Xử lý:**
                    - Cập nhật trạng thái thành SHIPPING
                    - Lưu thời gian bắt đầu vận chuyển
                    - User có thể xác nhận đã nhận hàng sau bước này
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(
            Authentication authentication,
            @Parameter(description = "ID đơn hàng") @PathVariable String orderId) {

        String vendorId = ((User) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.shipOrder(vendorId, orderId);

        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Xác nhận đã gửi hàng thành công")
                .result(response)
                .build());
    }

    @GetMapping("/shop/orders")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "[VENDOR] Lấy danh sách đơn hàng của shop",
            description = """
                    **Mục đích:** Vendor xem tất cả đơn hàng của shop mình
                    
                    **Hỗ trợ:**
                    - Phân trang
                    - Sắp xếp theo thời gian tạo
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getShopOrders(
            Authentication authentication,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "Sắp xếp theo") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Kiểu sắp xếp") @RequestParam(defaultValue = "desc") String sortType) {

        String vendorId = ((User) authentication.getPrincipal()).getId();

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<OrderResponse> response = orderService.getShopOrders(vendorId, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<OrderResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đơn hàng thành công")
                .result(response)
                .build());
    }

    @GetMapping("/shop/orders/by-status/{status}")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(
            summary = "[VENDOR] Lấy đơn hàng theo trạng thái",
            description = """
                    **Mục đích:** Vendor lọc đơn hàng theo trạng thái
                    
                    **Use case:**
                    - Xem đơn hàng chờ xác nhận (PENDING)
                    - Xem đơn hàng đã xác nhận (CONFIRMED) cần gửi hàng
                    - Theo dõi đơn hàng đang vận chuyển (SHIPPING)
                    - Xem đơn hàng đã hoàn thành (DELIVERED)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getShopOrdersByStatus(
            Authentication authentication,
            @Parameter(description = "Trạng thái đơn hàng") @PathVariable OrderStatus status,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "Sắp xếp theo") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Kiểu sắp xếp") @RequestParam(defaultValue = "desc") String sortType) {

        String vendorId = ((User) authentication.getPrincipal()).getId();

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<OrderResponse> response = orderService.getShopOrdersByStatus(vendorId, status, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<OrderResponse>>builder()
                .code(1000)
                .message("Lấy đơn hàng theo trạng thái thành công")
                .result(response)
                .build());
    }

    // ==================== ADMIN APIS ====================

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy tất cả đơn hàng theo hệ thống",
            description = """
                    **Mục đích:** Admin xem tất cả đơn hàng trong hệ thống với các bộ lọc tùy chọn
                    
                    **Các tham số lọc (tất cả optional):**
                    - userId: Lọc theo người dùng cụ thể
                    - vendorId: Lọc theo shop của vendor cụ thể
                    - status: Lọc theo trạng thái đơn hàng
                    
                    **Lưu ý:**
                    - Nếu không truyền tham số nào, trả về tất cả đơn hàng
                    - Sắp xếp theo thời gian tạo (mới nhất trước)
                    - Không phân trang
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrdersForAdmin(
            @Parameter(description = "ID người dùng (optional)") @RequestParam(required = false) String userId,
            @Parameter(description = "ID vendor (optional)") @RequestParam(required = false) String vendorId,
            @Parameter(description = "Trạng thái đơn hàng (optional)") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Số trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "Sắp xếp theo (createdAt, totalAmount, status)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Kiểu sắp xếp (asc/desc)") @RequestParam(defaultValue = "desc") String sortType) {

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        PageResponse<OrderResponse> response = orderService.getAllOrdersForAdmin(userId, vendorId, status, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<OrderResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đơn hàng thành công")
                .result(response)
                .build());
    }
}
