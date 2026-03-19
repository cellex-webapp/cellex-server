package com.example.cellex.services.order;

import com.example.cellex.dtos.request.order.*;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.order.AvailableCouponResponse;
import com.example.cellex.dtos.response.order.CheckoutResponse;
import com.example.cellex.dtos.response.order.OrderResponse;
import com.example.cellex.dtos.response.vnpay.VnpayPaymentResponse;
import com.example.cellex.enums.CouponStatus;
import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.PaymentMethod;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.cart.Cart;
import com.example.cellex.models.coupon.UserCoupon;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.cart.CartRepository;
import com.example.cellex.repositories.coupon.UserCouponRepository;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.user.UserService;
import com.example.cellex.services.shop.ShopService;
import com.example.cellex.services.payment.vnpay.VnpayService;
import com.example.cellex.services.notification.NotificationHelper;
import com.example.cellex.services.recommendation.UserInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserCouponRepository userCouponRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ShopService shopService;
    private final VnpayService vnpayService;
    private final NotificationHelper notificationHelper;
    private final UserInteractionService userInteractionService;

    @Transactional
    public OrderResponse createOrderFromProduct(String userId, CreateOrderRequest request) {
        log.info("Creating order from product for user: {}", userId);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.NO_PRODUCTS_SELECTED);
        }

        // Support multiple items provided from product page flow
        List<CreateOrderRequest.Item> items = request.getItems();

        // Validate and build order items
        if (items == null || items.isEmpty()) {
            throw new AppException(ErrorCode.NO_PRODUCTS_SELECTED);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double subtotal = 0.0;
        String shopId = null;

        for (CreateOrderRequest.Item it : items) {
            Product product = productRepository.findById(it.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            if (!product.getIsPublished()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_PUBLISHED);
            }

            if (product.getStockQuantity() < it.getQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            if (shopId == null) shopId = product.getShopId();
            else if (!shopId.equals(product.getShopId())) {
            throw new AppException(ErrorCode.PRODUCTS_MUST_BE_FROM_SAME_SHOP);
            }

            OrderItem orderItem = OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getImages() != null && !product.getImages().isEmpty()
                    ? product.getImages().get(0) : null)
                .priceDecimal(BigDecimal.valueOf(product.getFinalPrice()))
                .quantity(it.getQuantity())
                .subtotalDecimal(BigDecimal.valueOf(product.getFinalPrice() * it.getQuantity()))
                .build();

            orderItems.add(orderItem);
            subtotal += orderItem.getSubtotal();
        }

        // Lấy thông tin shop
        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        double shippingFee = 0.0;
        double totalAmount = subtotal + shippingFee;

        // Tạo đơn hàng (không lưu note ở bước tạo)
        Order order = Order.builder()
            .orderCode(generateOrderCode())
            .userUuid(UUID.fromString(userId))
            .shopUuid(UUID.fromString(shop.getId()))
            .shopName(shop.getShopName())
            .subtotalDecimal(BigDecimal.valueOf(subtotal))
            .shippingFeeDecimal(BigDecimal.valueOf(shippingFee))
            .discountAmountDecimal(BigDecimal.ZERO)
            .totalAmountDecimal(BigDecimal.valueOf(totalAmount))
            .status(OrderStatus.PENDING)
            // .note omitted here; note will be set on checkout
            .statusHistory(new ArrayList<>())
            .isPaid(false)
            .isFromCart(false)
            .build();

        // Set bidirectional relationship for JPA
        for (OrderItem item : orderItems) { item.setOrder(order); }
        order.setItems(orderItems);

        // Thêm lịch sử trạng thái
        addStatusHistory(order, OrderStatus.PENDING, "Đơn hàng được tạo", userId);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created from product successfully: {}", savedOrder.getId());

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse createOrderFromCart(String userId, CreateOrderRequest request) {
        log.info("Creating order from cart for user: {}", userId);

        // Lấy giỏ hàng
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        if (cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        // Map requested quantities by productId
        Map<String, Integer> requestedQty = request.getItems().stream()
                .collect(Collectors.toMap(CreateOrderRequest.Item::getProductId,
                        CreateOrderRequest.Item::getQuantity));

        // Validate requested items exist in cart
        List<Cart.CartItem> selectedItems = cart.getItems().stream()
                .filter(item -> requestedQty.containsKey(item.getProductId()))
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            throw new AppException(ErrorCode.NO_PRODUCTS_SELECTED);
        }

        // Kiểm tra tất cả sản phẩm phải cùng shop
        String shopId = selectedItems.get(0).getShopId();
        boolean allSameShop = selectedItems.stream()
                .allMatch(item -> item.getShopId().equals(shopId));

        if (!allSameShop) {
            throw new AppException(ErrorCode.PRODUCTS_MUST_BE_FROM_SAME_SHOP);
        }

        // Lấy thông tin shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Tạo order items và kiểm tra tồn kho
        List<OrderItem> orderItems = new ArrayList<>();
        double subtotal = 0.0;

        for (Cart.CartItem cartItem : selectedItems) {
            String pid = cartItem.getProductId();
            int qty = requestedQty.getOrDefault(pid, cartItem.getQuantity());

            // Ensure requested quantity does not exceed quantity in cart
            if (qty > cartItem.getQuantity()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            if (!product.getIsPublished()) {
                throw new AppException(ErrorCode.PRODUCT_NOT_PUBLISHED);
            }

            if (product.getStockQuantity() < qty) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productImage(product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0) : null)
                    .priceDecimal(BigDecimal.valueOf(product.getFinalPrice()))
                    .quantity(qty)
                    .subtotalDecimal(BigDecimal.valueOf(product.getFinalPrice() * qty))
                    .build();

            orderItems.add(orderItem);
            subtotal += orderItem.getSubtotal();
        }

        // Tạo đơn hàng (không lưu note ở bước tạo)
        double shippingFee = 0.0;
        double totalAmount = subtotal + shippingFee;

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .userUuid(UUID.fromString(userId))
                .shopUuid(UUID.fromString(shop.getId()))
                .shopName(shop.getShopName())
                .subtotalDecimal(BigDecimal.valueOf(subtotal))
                .shippingFeeDecimal(BigDecimal.valueOf(shippingFee))
                .discountAmountDecimal(BigDecimal.ZERO)
                .totalAmountDecimal(BigDecimal.valueOf(totalAmount))
                .status(OrderStatus.PENDING)
                // .note omitted here; note will be set on checkout
                .statusHistory(new ArrayList<>())
                .isPaid(false)
                .isFromCart(true) // Đánh dấu đơn hàng từ giỏ hàng
                .build();

        // Set bidirectional relationship for JPA
        for (OrderItem item : orderItems) { item.setOrder(order); }
        order.setItems(orderItems);

        addStatusHistory(order, OrderStatus.PENDING, "Đơn hàng được tạo", userId);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created from cart successfully: {}", savedOrder.getId());

        return mapToResponse(savedOrder);
    }

    public List<AvailableCouponResponse> getAvailableCoupons(String userId, String orderId) {
        log.info("Getting available coupons for order: {}", orderId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Lấy tất cả coupon active của user
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndStatus(userId, CouponStatus.ACTIVE);

        // Lọc và tính toán cho từng coupon
        return userCoupons.stream()
                .map(coupon -> {
                    boolean canApply = canApplyCoupon(coupon, order);
                    double discountPreview = canApply ? calculateDiscount(coupon, order.getSubtotal()) : 0.0;

                    return AvailableCouponResponse.builder()
                            .id(coupon.getId())
                            .code(coupon.getCode())
                            .title(coupon.getTitle())
                            .description(coupon.getDescription())
                            .couponType(coupon.getCouponType())
                            .discountValue(coupon.getDiscountValue())
                            .minOrderAmount(coupon.getMinOrderAmount())
                            .expiresAt(coupon.getExpiresAt())
                            .status(coupon.getStatus())
                            .canApply(canApply)
                            .discountPreview(discountPreview)
                            .build();
                })
                .sorted((c1, c2) -> {
                    if (c1.getCanApply() && !c2.getCanApply()) return -1;
                    if (!c1.getCanApply() && c2.getCanApply()) return 1;
                    return Double.compare(c2.getDiscountPreview(), c1.getDiscountPreview());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse applyCoupon(String userId, String orderId, ApplyCouponRequest request) {
        log.info("Applying coupon to order: {}", orderId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Chỉ cho phép áp dụng coupon khi đơn hàng ở trạng thái PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.CANNOT_APPLY_COUPON_TO_THIS_ORDER);
        }

        // Tìm coupon theo code VÀ userId để tránh duplicate
        UserCoupon coupon = userCouponRepository.findByCodeAndUserId(request.getCouponCode(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        // Kiểm tra trạng thái và hạn sử dụng
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new AppException(ErrorCode.COUPON_NOT_ACTIVE);
        }

        if (coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.COUPON_EXPIRED);
        }

        // Kiểm tra điều kiện áp dụng
        if (!canApplyCoupon(coupon, order)) {
            throw new AppException(ErrorCode.COUPON_NOT_APPLICABLE);
        }

        // Tính giảm giá
        double discountAmount = calculateDiscount(coupon, order.getSubtotal());
        double newTotalAmount = order.getSubtotal() + order.getShippingFee() - discountAmount;

        // Cập nhật order
        order.setUserCouponId(coupon.getId());
        order.setCouponCode(coupon.getCode());
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(newTotalAmount);

        Order updated = orderRepository.save(order);
        log.info("Coupon applied successfully to order: {}", orderId);

        return mapToResponse(updated);
    }

    @Transactional
    public OrderResponse removeCoupon(String userId, String orderId) {
        log.info("Removing coupon from order: {}", orderId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_THIS_ORDER);
        }

        // Bỏ coupon
        order.setUserCouponId(null);
        order.setCouponCode(null);
        order.setDiscountAmount(0.0);
        order.setTotalAmount(order.getSubtotal() + order.getShippingFee());

        Order updated = orderRepository.save(order);
        log.info("Coupon removed from order: {}", orderId);

        return mapToResponse(updated);
    }

    @Transactional
    public OrderResponse checkoutOrder(String userId, String orderId, CheckoutOrderRequest request) {
        log.info("Checking out order: {}", orderId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PROCESSED);
        }

        // Lấy thông tin user để lấy địa chỉ
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Tạo shipping address từ user address
        Order.ShippingAddress shippingAddress = Order.ShippingAddress.builder()
                .provinceCode(user.getAddress() != null ? user.getAddress().getProvinceCode() : null)
                .provinceName(user.getAddress() != null ? user.getAddress().getProvinceName() : null)
                .communeCode(user.getAddress() != null ? user.getAddress().getCommuneCode() : null)
                .communeName(user.getAddress() != null ? user.getAddress().getCommuneName() : null)
                .detailAddress(user.getAddress() != null ? user.getAddress().getDetailAddress() : null)
                .fullAddress(user.getAddress() != null ? user.getAddress().getFullAddress() : null)
                .build();

        // Cập nhật order
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNote(request.getNote()); // Set note tại bước checkout

        // Giảm stock quantity của các sản phẩm
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        // Nếu có coupon, đánh dấu đã sử dụng
        if (order.getUserCouponId() != null) {
            UserCoupon coupon = userCouponRepository.findById(order.getUserCouponId())
                    .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

            coupon.setStatus(CouponStatus.REDEEMED);
            coupon.setRedeemedOrderId(order.getId());
            coupon.setRedeemedAt(LocalDateTime.now());
            userCouponRepository.save(coupon);
        }

        // Xóa/giảm số lượng các sản phẩm đã đặt khỏi giỏ hàng (nếu đặt từ giỏ hàng)
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            for (OrderItem orderedItem : order.getItems()) {
                cart.getItems().stream()
                        .filter(ci -> ci.getProductId().equals(orderedItem.getProductId()))
                        .findFirst()
                        .ifPresent(ci -> {
                            int remaining = ci.getQuantity() - orderedItem.getQuantity();
                            if (remaining > 0) {
                                ci.setQuantity(remaining);
                            } else {
                                cart.getItems().remove(ci);
                            }
                        });
            }

            // Tính lại tổng giỏ hàng
            double totalPrice = cart.getItems().stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
            int totalQuantity = cart.getItems().stream()
                    .mapToInt(Cart.CartItem::getQuantity)
                    .sum();

            cart.setTotalPrice(totalPrice);
            cart.setTotalQuantity(totalQuantity);
            cart.setUpdatedAt(LocalDateTime.now());

            cartRepository.save(cart);
        });

        Order updated = orderRepository.save(order);
        log.info("Order checked out successfully: {}", orderId);

        return mapToResponse(updated);
    }

    @Transactional
    public CheckoutResponse checkoutOrderWithPayment(
            String userId, 
            String orderId, 
            CheckoutOrderRequest request,
            String ipAddress
    ) {
        log.info("Checking out order with payment: {}", orderId);

        // First perform regular checkout
        OrderResponse orderResponse = checkoutOrder(userId, orderId, request);

        // If payment method is VNPAY, create payment URL
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            String orderInfo = "Thanh toan don hang " + orderId;
            Long amount = order.getTotalAmount().longValue();

            VnpayPaymentResponse vnpayResponse = vnpayService.createPaymentUrl(
                    orderId,
                    amount,
                    orderInfo,
                    ipAddress,
                    "vn"
            );

            if (!"00".equals(vnpayResponse.getCode())) {
                throw new AppException(ErrorCode.PAYMENT_ERROR);
            }

            return CheckoutResponse.builder()
                    .order(orderResponse)
                    .paymentUrl(vnpayResponse.getPaymentUrl())
                    .message("Vui lòng thanh toán qua VNPAY")
                    .build();
        }

        // For COD, return order without payment URL
        return CheckoutResponse.builder()
                .order(orderResponse)
                .message("Đặt hàng thành công")
                .build();
    }

    @Transactional
    public OrderResponse cancelOrder(String userId, String orderId) {
        log.info("Cancelling order: {}", orderId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Chỉ cho phép hủy khi đơn hàng ở trạng thái PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_ORDER);
        }

        // Hoàn lại stock quantity
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        // Hoàn lại coupon nếu có
        if (order.getUserCouponId() != null) {
            UserCoupon coupon = userCouponRepository.findById(order.getUserCouponId())
                    .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

            coupon.setStatus(CouponStatus.ACTIVE);
            coupon.setRedeemedOrderId(null);
            coupon.setRedeemedAt(null);
            userCouponRepository.save(coupon);
        }

        // Cập nhật trạng thái
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Khách hàng hủy");
        order.setCancelledAt(LocalDateTime.now());
        addStatusHistory(order, OrderStatus.CANCELLED, "Khách hàng hủy", userId);

        Order updated = orderRepository.save(order);
        log.info("Order cancelled successfully: {}", orderId);

        // Gửi notification và email cho user
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user != null) {
            notificationHelper.notifyOrderCancelled(updated, user, "Khách hàng hủy");
        }

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteOrder(String userId, String orderId) {
        log.info("Deleting order: {}", orderId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Chỉ cho phép xóa khi đơn hàng ở trạng thái PENDING hoặc CANCELLED
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.CANNOT_DELETE_ORDER);
        }

        // Hoàn lại stock quantity nếu đơn hàng chưa bị hủy (status = PENDING)
        if (order.getStatus() == OrderStatus.PENDING) {
            for (OrderItem item : order.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }

            // Hoàn lại coupon nếu có
            if (order.getUserCouponId() != null) {
                UserCoupon coupon = userCouponRepository.findById(order.getUserCouponId())
                        .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

                coupon.setStatus(CouponStatus.ACTIVE);
                coupon.setRedeemedOrderId(null);
                coupon.setRedeemedAt(null);
                userCouponRepository.save(coupon);
            }
        }

        // Nếu đơn hàng được tạo từ giỏ hàng, hoàn lại sản phẩm vào giỏ hàng
        if (Boolean.TRUE.equals(order.getIsFromCart())) {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        // Tạo giỏ hàng mới nếu chưa có
                        Cart newCart = Cart.builder()
                                .userId(userId)
                                .items(new ArrayList<>())
                                .totalPrice(0.0)
                                .totalQuantity(0)
                                .build();
                        return cartRepository.save(newCart);
                    });

            // Thêm lại các sản phẩm vào giỏ hàng
            for (OrderItem orderItem : order.getItems()) {
                // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
                Optional<Cart.CartItem> existingItem = cart.getItems().stream()
                        .filter(item -> item.getProductId().equals(orderItem.getProductId()))
                        .findFirst();

                if (existingItem.isPresent()) {
                    // Nếu đã có, tăng số lượng
                    existingItem.get().setQuantity(existingItem.get().getQuantity() + orderItem.getQuantity());
                } else {
                    // Nếu chưa có, thêm mới
                    // Ensure product exists
                    productRepository.findById(orderItem.getProductId())
                            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                    Cart.CartItem cartItem = Cart.CartItem.builder()
                            .productId(orderItem.getProductId())
                            .productName(orderItem.getProductName())
                            .productImage(orderItem.getProductImage())
                            .shopId(order.getShopId())
                            .shopName(order.getShopName())
                            .price(orderItem.getPrice())
                            .quantity(orderItem.getQuantity())
                            .build();

                    cart.getItems().add(cartItem);
                }
            }

            // Tính lại tổng giỏ hàng
            double totalPrice = cart.getItems().stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
            int totalQuantity = cart.getItems().stream()
                    .mapToInt(Cart.CartItem::getQuantity)
                    .sum();

            cart.setTotalPrice(totalPrice);
            cart.setTotalQuantity(totalQuantity);
            cart.setUpdatedAt(LocalDateTime.now());

            cartRepository.save(cart);
            log.info("Restored {} items to cart for user: {}", order.getItems().size(), userId);
        }

        // Xóa đơn hàng
        orderRepository.delete(order);
        log.info("Order deleted successfully: {}", orderId);
    }

    // VENDOR APIs
    @Transactional
    public OrderResponse confirmOrder(String vendorId, String orderId) {
        log.info("Vendor {} confirming order: {}", vendorId, orderId);

        Order order = findOrderByVendor(vendorId, orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PROCESSED);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        addStatusHistory(order, OrderStatus.CONFIRMED, "Đơn hàng đã được xác nhận", vendorId);

        Order updated = orderRepository.save(order);
        log.info("Order confirmed successfully: {}", orderId);

        // Gửi notification và email cho user
        User user = userRepository.findById(order.getUserId())
                .orElse(null);
        if (user != null) {
            notificationHelper.notifyOrderConfirmed(updated, user);
        }

        return mapToResponse(updated);
    }

    @Transactional
    public OrderResponse shipOrder(String vendorId, String orderId) {
        log.info("Vendor {} shipping order: {}", vendorId, orderId);

        Order order = findOrderByVendor(vendorId, orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new AppException(ErrorCode.ORDER_NOT_CONFIRMED);
        }

        order.setStatus(OrderStatus.SHIPPING);
        order.setShippingAt(LocalDateTime.now());
        addStatusHistory(order, OrderStatus.SHIPPING, "Đơn hàng đang được vận chuyển", vendorId);

        Order updated = orderRepository.save(order);
        log.info("Order shipped successfully: {}", orderId);

        // Gửi notification và email cho user
        User user = userRepository.findById(order.getUserId())
                .orElse(null);
        if (user != null) {
            notificationHelper.notifyOrderShipping(updated, user);
        }

        return mapToResponse(updated);
    }

    // USER API
    @Transactional
    public OrderResponse confirmDelivery(String userId, String orderId) {
        log.info("User {} confirming delivery for order: {}", userId, orderId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new AppException(ErrorCode.ORDER_NOT_SHIPPING);
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId())
                    .ifPresent(product -> userInteractionService.recordPurchase(userId, item.getProductId(), product.getCategoryId()));
        }

        // Nếu phương thức thanh toán là COD, cập nhật is_paid và paid_at
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setIsPaid(true);
            order.setPaidAt(LocalDateTime.now());
        }

        addStatusHistory(order, OrderStatus.DELIVERED, "Đơn hàng đã được giao thành công", userId);

        Order updated = orderRepository.save(order);
        log.info("Order delivered successfully: {}", orderId);

        // Gửi notification và email cho user
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user != null) {
            notificationHelper.notifyOrderDelivered(updated, user);
        }

        return mapToResponse(updated);
    }

    public PageResponse<OrderResponse> getMyOrders(String userId, Pageable pageable) {
        log.info("Getting orders for user: {}", userId);
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return PageResponse.of(orders, this::mapToResponse);
    }

    public PageResponse<OrderResponse> getMyOrdersByStatus(String userId, OrderStatus status, Pageable pageable) {
        log.info("Getting orders for user {} with status: {}", userId, status);
        Page<Order> orders = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        return PageResponse.of(orders, this::mapToResponse);
    }

    /**
     * Get an order by id with role-based access control.
     * - ADMIN: can fetch any order
     * - VENDOR: can fetch orders that belong to the vendor's shop
     * - USER: can fetch only their own orders
     */
    public OrderResponse getOrderById(String requesterId, Role role, String orderId) {
        log.info("Getting order {} for requester: {} with role: {}", orderId, requesterId, role);

        Order order;

        if (role == Role.ADMIN) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        } else if (role == Role.VENDOR) {
            // Ensure vendor has a shop and the order belongs to that shop
            Shop shop = shopRepository.findByVendorId(requesterId)
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            order = orderRepository.findByIdAndShopId(orderId, shop.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        } else {
            // Default to USER behavior
            order = orderRepository.findByIdAndUserId(orderId, requesterId)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        }

        return mapToResponse(order);
    }

    // VENDOR APIs
    public PageResponse<OrderResponse> getShopOrders(String vendorId, Pageable pageable) {
        log.info("Getting orders for vendor: {}", vendorId);
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        Page<Order> orders = orderRepository.findByShopId(shop.getId(), pageable);
        return PageResponse.of(orders, this::mapToResponse);
    }

    public PageResponse<OrderResponse> getShopOrdersByStatus(String vendorId, OrderStatus status, Pageable pageable) {
        log.info("Getting orders for vendor {} with status: {}", vendorId, status);
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        Page<Order> orders = orderRepository.findByShopIdAndStatus(shop.getId(), status, pageable);
        return PageResponse.of(orders, this::mapToResponse);
    }

    // ADMIN APIs
    public PageResponse<OrderResponse> getAllOrdersForAdmin(String userId, String vendorId, OrderStatus status, Pageable pageable) {
        log.info("Admin getting all orders with filters - userId: {}, vendorId: {}, status: {}", userId, vendorId, status);

        Page<Order> page;

        // Nếu có userId, lọc theo user
        if (userId != null && !userId.isEmpty()) {
            if (status != null) {
                page = orderRepository.findByUserIdAndStatus(userId, status, pageable);
            } else {
                page = orderRepository.findByUserId(userId, pageable);
            }
        }
        // Nếu có vendorId, lọc theo shop của vendor
        else if (vendorId != null && !vendorId.isEmpty()) {
            Shop shop = shopRepository.findByVendorId(vendorId)
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (status != null) {
                page = orderRepository.findByShopIdAndStatus(shop.getId(), status, pageable);
            } else {
                page = orderRepository.findByShopId(shop.getId(), pageable);
            }
        }
        // Nếu chỉ có status, lọc theo status
        else if (status != null) {
            page = orderRepository.findByStatus(status, pageable);
        }
        // Không có filter nào, lấy tất cả
        else {
            page = orderRepository.findAll(pageable);
        }

        return PageResponse.of(page, this::mapToResponse);
    }

    // Helper methods
    private Order findOrderByVendor(String vendorId, String orderId) {
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        return orderRepository.findByIdAndShopId(orderId, shop.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }

    private boolean canApplyCoupon(UserCoupon coupon, Order order) {
        // Kiểm tra điều kiện tối thiểu
        if (coupon.getMinOrderAmount() != null && order.getSubtotal() < coupon.getMinOrderAmount()) {
            return false;
        }

        // Kiểm tra áp dụng cho sản phẩm/danh mục cụ thể
        if (coupon.getApplicableProductIds() != null && !coupon.getApplicableProductIds().isEmpty()) {
            boolean hasApplicableProduct = order.getItems().stream()
                    .anyMatch(item -> coupon.getApplicableProductIds().contains(item.getProductId()));
            if (!hasApplicableProduct) {
                return false;
            }
        }

        if (coupon.getApplicableCategoryIds() != null && !coupon.getApplicableCategoryIds().isEmpty()) {
            // Cần kiểm tra category của sản phẩm
            // Có thể cần thêm categoryId vào OrderItem hoặc query từ Product
            // Tạm thời bỏ qua logic này
        }

        return true;
    }

    private double calculateDiscount(UserCoupon coupon, double subtotal) {
        if (coupon.getCouponType() == CouponType.PERCENTAGE) {
            return subtotal * coupon.getDiscountValue() / 100.0;
        } else if (coupon.getCouponType() == CouponType.FIXED) {
            return Math.min(coupon.getDiscountValue(), subtotal);
        }
        return 0.0;
    }

    private void addStatusHistory(Order order, OrderStatus status, String note, String updatedBy) {
        Order.StatusHistory history = Order.StatusHistory.builder()
                .status(status)
                .note(note)
                .updatedBy(updatedBy)
                .updatedAt(LocalDateTime.now())
                .build();

        if (order.getStatusHistory() == null) {
            order.setStatusHistory(new ArrayList<>());
        }
        order.getStatusHistory().add(history);
    }

    private OrderResponse mapToResponse(Order order) {
        // Fetch full user and shop responses; if not found, fall back to null
        com.example.cellex.dtos.response.user.UserResponse userResp = null;
        com.example.cellex.dtos.response.shop.ShopResponse shopResp = null;
        try {
            if (order.getUserId() != null) userResp = userService.getUserById(order.getUserId());
        } catch (Exception ignored) {}
        try {
            if (order.getShopId() != null) shopResp = shopService.getShopById(order.getShopId());
        } catch (Exception ignored) {}

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .user(userResp)
                .shop(shopResp)
                .items(order.getItems().stream()
                        .map(this::mapItemToResponse)
                        .collect(Collectors.toList()))
                .shippingAddress(order.getShippingAddress() != null
                        ? mapShippingAddressToResponse(order.getShippingAddress()) : null)
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .paymentMethod(order.getPaymentMethod())
                .isPaid(order.getIsPaid())
                .paidAt(order.getPaidAt())
                .status(order.getStatus())
                .statusHistory(order.getStatusHistory() != null
                        ? order.getStatusHistory().stream()
                                .map(this::mapStatusHistoryToResponse)
                                .collect(Collectors.toList())
                        : null)
                .note(order.getNote())
                .cancelReason(order.getCancelReason())
                .cancelledAt(order.getCancelledAt())
                .confirmedAt(order.getConfirmedAt())
                .shippingAt(order.getShippingAt())
                .deliveredAt(order.getDeliveredAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderResponse.OrderItemResponse mapItemToResponse(OrderItem item) {
        return OrderResponse.OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    private OrderResponse.ShippingAddressResponse mapShippingAddressToResponse(Order.ShippingAddress address) {
        return OrderResponse.ShippingAddressResponse.builder()
                .street(address.getDetailAddress())
                .commune(address.getCommuneName())
                .province(address.getProvinceName())
                .country("Việt Nam")
                .fullAddress(address.getFullAddress())
                .build();
    }

    private OrderResponse.StatusHistoryResponse mapStatusHistoryToResponse(Order.StatusHistory history) {
        return OrderResponse.StatusHistoryResponse.builder()
                .status(history.getStatus())
                .note(history.getNote())
                .updatedBy(history.getUpdatedBy())
                .updatedAt(history.getUpdatedAt())
                .build();
    }

    /**
     * Generate a unique order code in format: ORDYYYYMMDDHHmmss + 4 random digits
     * Example: ORD20240115143052-1234
     */
    private String generateOrderCode() {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        int randomSuffix = new java.util.Random().nextInt(9000) + 1000; // 4-digit random number (1000-9999)
        return String.format("ORD%s-%04d", timestamp, randomSuffix);
    }

    /**
     * Migration method to generate orderCode for existing orders without one.
     * Should be called on application startup.
     */
    @Transactional
    public void migrateOrderCodes() {
        log.info("Starting migration of order codes for existing orders...");
        
        List<Order> ordersWithoutCode = orderRepository.findAll().stream()
                .filter(order -> order.getOrderCode() == null || order.getOrderCode().isEmpty())
                .collect(Collectors.toList());
        
        if (ordersWithoutCode.isEmpty()) {
            log.info("No orders need migration for order codes.");
            return;
        }
        
        log.info("Found {} orders without order codes, starting migration...", ordersWithoutCode.size());
        
        for (Order order : ordersWithoutCode) {
            String newCode = generateOrderCode();
            // Ensure uniqueness by checking and regenerating if needed
            while (orderRepository.findByOrderCode(newCode).isPresent()) {
                newCode = generateOrderCode();
            }
            order.setOrderCode(newCode);
            orderRepository.save(order);
        }
        
        log.info("Completed migration of {} order codes.", ordersWithoutCode.size());
    }
}

