package com.example.cellex.services.cart;

import com.example.cellex.dtos.request.cart.AddToCartRequest;
import com.example.cellex.dtos.response.cart.CartResponse;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.cart.Cart;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.repositories.cart.CartRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    /**
     * Thêm sản phẩm vào giỏ hàng (tự lấy user id qua jwt)
     */
    @Transactional
    public CartResponse addToCart(String userId, AddToCartRequest request) {
        log.info("Adding product {} to cart for user {}", request.getProductId(), userId);

        // Kiểm tra sản phẩm có tồn tại không
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Kiểm tra sản phẩm đã được publish chưa
        if (!product.getIsPublished()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // Kiểm tra stock
        if (product.getStockQuantity() <= 0) {
            throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        // Lấy thông tin shop
        Shop shop = shopRepository.findById(product.getShopId()).orElse(null);
        String shopName = shop != null ? shop.getShopName() : "Unknown Shop";

        // Tìm hoặc tạo giỏ hàng cho user
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder()
                        .userId(userId)
                        .items(new ArrayList<>())
                        .totalPrice(0.0)
                        .totalQuantity(0)
                        .createdAt(LocalDateTime.now())
                        .build());

        // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
        boolean itemExists = false;
        for (Cart.CartItem item : cart.getItems()) {
            if (item.getProductId().equals(request.getProductId())) {
                // Cập nhật số lượng
                int newQuantity = item.getQuantity() + request.getQuantity();

                // Kiểm tra stock với số lượng mới
                if (product.getStockQuantity() < newQuantity) {
                    throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
                }

                item.setQuantity(newQuantity);
                itemExists = true;
                log.info("Updated quantity for product {} in cart to {}", request.getProductId(), newQuantity);
                break;
            }
        }

        // Nếu chưa có thì thêm mới
        if (!itemExists) {
            String productImage = (product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0)
                    : null;

            Cart.CartItem newItem = Cart.CartItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productImage(productImage)
                    .quantity(request.getQuantity())
                    .price(product.getFinalPrice() != null ? product.getFinalPrice() : product.getPrice())
                    .shopId(product.getShopId())
                    .shopName(shopName)
                    .build();

            cart.getItems().add(newItem);
            log.info("Added new product {} to cart", request.getProductId());
        }

        // Cập nhật tổng giá và số lượng
        recalculateCartTotals(cart);
        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart updated successfully for user {}", userId);

        return mapToCartResponse(savedCart);
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng (hỗ trợ xóa nhiều sản phẩm 1 lúc)
     */
    @Transactional
    public CartResponse removeFromCart(String userId, List<String> productIds) {
        log.info("Removing {} products from cart for user {}", productIds.size(), userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        // Xóa các sản phẩm theo danh sách productIds
        int removedCount = 0;
        List<Cart.CartItem> updatedItems = new ArrayList<>();

        for (Cart.CartItem item : cart.getItems()) {
            if (!productIds.contains(item.getProductId())) {
                updatedItems.add(item);
            } else {
                removedCount++;
            }
        }

        if (removedCount == 0) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        cart.setItems(updatedItems);

        // Cập nhật tổng giá và số lượng
        recalculateCartTotals(cart);
        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        log.info("Removed {} products from cart for user {}", removedCount, userId);

        return mapToCartResponse(savedCart);
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng
     */
    @Transactional
    public CartResponse clearCart(String userId) {
        log.info("Clearing cart for user {}", userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cart.setTotalQuantity(0);
        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart cleared successfully for user {}", userId);

        return mapToCartResponse(savedCart);
    }

    /**
     * Get giỏ hàng của tôi (tự lấy user id theo jwt)
     */
    public CartResponse getMyCart(String userId) {
        log.info("Getting cart for user {}", userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Tạo giỏ hàng trống nếu chưa có
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .items(new ArrayList<>())
                            .totalPrice(0.0)
                            .totalQuantity(0)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return cartRepository.save(newCart);
                });

        return mapToCartResponse(cart);
    }

    /**
     * Get giỏ hàng theo id (dành cho admin)
     */
    public CartResponse getCartById(String cartId) {
        log.info("Getting cart by id {}", cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        return mapToCartResponse(cart);
    }

    /**
     * Get tất cả giỏ hàng (dành cho admin) - without pagination
     */
    public List<CartResponse> getAllCarts() {
        log.info("Getting all carts without pagination");

        List<Cart> carts = cartRepository.findAll();
        return carts.stream().map(this::mapToCartResponse).collect(Collectors.toList());
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng (tăng/giảm 1)
     */
    @Transactional
    public CartResponse updateCartItemQuantity(String userId, String productId, boolean isIncrease) {
        log.info("Updating quantity for product {} in cart for user {}, action: {}",
                productId, userId, isIncrease ? "INCREASE" : "DECREASE");

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        // Tìm sản phẩm trong giỏ hàng
        Cart.CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        // Kiểm tra sản phẩm còn tồn tại và đủ stock
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (isIncrease) {
            // Tăng số lượng lên 1
            int newQuantity = cartItem.getQuantity() + 1;

            // Kiểm tra stock
            if (product.getStockQuantity() < newQuantity) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            cartItem.setQuantity(newQuantity);
            log.info("Increased quantity to {}", newQuantity);
        } else {
            // Giảm số lượng xuống 1
            int newQuantity = cartItem.getQuantity() - 1;

            if (newQuantity <= 0) {
                // Nếu số lượng <= 0 thì xóa sản phẩm khỏi giỏ hàng
                cart.getItems().remove(cartItem);
                log.info("Removed product from cart as quantity reached 0");
            } else {
                cartItem.setQuantity(newQuantity);
                log.info("Decreased quantity to {}", newQuantity);
            }
        }

        // Cập nhật tổng giá và số lượng
        recalculateCartTotals(cart);
        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart updated successfully for user {}", userId);

        return mapToCartResponse(savedCart);
    }

    /**
     * Thiết lập số lượng cụ thể cho sản phẩm trong giỏ hàng
     */
    @Transactional
    public CartResponse setCartItemQuantity(String userId, String productId, Integer quantity) {
        log.info("Setting quantity for product {} in cart for user {} to {}", productId, userId, quantity);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        // Tìm sản phẩm trong giỏ hàng
        Cart.CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (quantity == 0) {
            // Nếu quantity = 0 thì xóa sản phẩm khỏi giỏ hàng
            cart.getItems().remove(cartItem);
            log.info("Removed product from cart as quantity is 0");
        } else {
            // Kiểm tra sản phẩm còn tồn tại và đủ stock
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            // Kiểm tra stock
            if (product.getStockQuantity() < quantity) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            cartItem.setQuantity(quantity);
            log.info("Updated quantity to {}", quantity);
        }

        // Cập nhật tổng giá và số lượng
        recalculateCartTotals(cart);
        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart updated successfully for user {}", userId);

        return mapToCartResponse(savedCart);
    }

    /**
     * Tính lại tổng giá và số lượng của giỏ hàng
     */
    private void recalculateCartTotals(Cart cart) {
        double totalPrice = 0.0;
        int totalQuantity = 0;

        for (Cart.CartItem item : cart.getItems()) {
            totalPrice += item.getPrice() * item.getQuantity();
            totalQuantity += item.getQuantity();
        }

        cart.setTotalPrice(totalPrice);
        cart.setTotalQuantity(totalQuantity);
    }

    /**
     * Map Cart entity to CartResponse DTO
     */
    private CartResponse mapToCartResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    // Lấy thông tin sản phẩm hiện tại để kiểm tra stock
                    Product product = productRepository.findById(item.getProductId()).orElse(null);

                    Integer availableStock = null;
                    boolean isAvailable;

                    if (product != null) {
                        availableStock = product.getStockQuantity();
                        isAvailable = product.getIsPublished() && product.getStockQuantity() > 0;
                    } else {
                        isAvailable = false;
                    }

                    return CartResponse.CartItemResponse.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .productImage(item.getProductImage())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .subtotal(item.getPrice() * item.getQuantity())
                            .shopId(item.getShopId())
                            .shopName(item.getShopName())
                            .availableStock(availableStock)
                            .isAvailable(isAvailable)
                            .build();
                })
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .totalPrice(cart.getTotalPrice())
                .totalQuantity(cart.getTotalQuantity())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
