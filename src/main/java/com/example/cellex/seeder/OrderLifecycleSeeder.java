package com.example.cellex.seeder;

import com.example.cellex.enums.CouponStatus;
import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.PaymentMethod;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderLifecycleSeeder {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserCouponRepository userCouponRepository;
    private final CartRepository cartRepository;
    private final UserBehaviorSimulator userBehaviorSimulator;
    private final ProductPopularityTracker productPopularityTracker;

    @Transactional
    public List<Order> seedOrdersForUser(User user, List<Shop> shops, List<Product> products) {
        if (user == null || user.getId() == null) {
            return List.of();
        }

        List<Product> sourceProducts = resolveProducts(products);
        List<Shop> sourceShops = resolveShops(shops);

        if (sourceProducts.isEmpty() || sourceShops.isEmpty()) {
            return List.of();
        }

        int targetOrderCount = userBehaviorSimulator.getOrderCount(user.getId());
        if (targetOrderCount <= 0) {
            return List.of();
        }

        Map<String, Shop> shopsById = sourceShops.stream()
                .filter(shop -> shop.getId() != null)
                .collect(Collectors.toMap(Shop::getId, shop -> shop, (left, right) -> left, HashMap::new));

        Map<String, List<Product>> productsByShopId = sourceProducts.stream()
                .filter(product -> product.getShopId() != null)
                .collect(Collectors.groupingBy(Product::getShopId));

        List<String> preferredCategories = userBehaviorSimulator.getPreferredCategories(user.getId());
        List<UserCoupon> activeCoupons = userCouponRepository.findByUserIdAndStatus(user.getId(), CouponStatus.ACTIVE);

        List<Order> seededOrders = new ArrayList<>();

        for (int i = 0; i < targetOrderCount; i++) {
            LocalDateTime createdAt = generateRecentWeightedCreatedAt();
            PaymentMethod paymentMethod = resolvePaymentMethod();
            OrderStatus status = resolveOrderStatus();

            OrderDraft draft = buildOrderDraft(
                    user,
                    sourceProducts,
                    productsByShopId,
                    shopsById,
                    preferredCategories
            );

            if (draft == null || draft.items().isEmpty() || draft.shop() == null) {
                continue;
            }

            CouponSelection couponSelection = selectCoupon(
                    activeCoupons,
                    draft.items(),
                    draft.productsByItemId(),
                    createdAt,
                    draft.subtotal().doubleValue(),
                    draft.shippingFee().doubleValue()
            );

            Order order = createOrder(
                    user,
                    draft,
                    status,
                    paymentMethod,
                    createdAt,
                    couponSelection,
                    cartRepository.existsByUserId(user.getId())
            );

            seededOrders.add(order);
        }

        if (seededOrders.isEmpty()) {
            return List.of();
        }

        List<Order> saved = orderRepository.saveAll(seededOrders);
        log.info("Seeded orders for userId={}: {}", user.getId(), saved.size());
        return saved;
    }

    private OrderDraft buildOrderDraft(
            User user,
            List<Product> products,
            Map<String, List<Product>> productsByShopId,
            Map<String, Shop> shopsById,
            List<String> preferredCategories
    ) {
        String preferredCategory = pickPreferredCategory(preferredCategories);
        Product firstProduct = productPopularityTracker.selectProduct(products, preferredCategory);

        if (firstProduct == null || firstProduct.getShopId() == null) {
            return null;
        }

        List<Product> shopProducts = productsByShopId.getOrDefault(firstProduct.getShopId(), List.of());
        if (shopProducts.isEmpty()) {
            return null;
        }

        Shop shop = shopsById.get(firstProduct.getShopId());
        if (shop == null) {
            shop = shopRepository.findById(firstProduct.getShopId()).orElse(null);
            if (shop == null) {
                return null;
            }
        }

        int itemCount = ThreadLocalRandom.current().nextInt(1, 4);
        Set<String> chosenProductIds = new HashSet<>();
        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<String, Product> productsByItemId = new HashMap<>();

        for (int i = 0; i < itemCount; i++) {
            Product selected = selectUniqueProduct(shopProducts, preferredCategory, chosenProductIds);
            if (selected == null) {
                continue;
            }

            int quantity = ThreadLocalRandom.current().nextInt(1, 4);
            double unitPrice = resolveProductPrice(selected);
            BigDecimal lineSubtotal = BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(quantity));

            OrderItem item = OrderItem.builder()
                    .productId(selected.getId())
                    .productName(selected.getName())
                    .productImage(resolveProductImage(selected))
                    .priceDecimal(BigDecimal.valueOf(unitPrice))
                    .quantity(quantity)
                    .subtotalDecimal(lineSubtotal)
                    .build();

            items.add(item);
            subtotal = subtotal.add(lineSubtotal);
            productsByItemId.put(selected.getId(), selected);
        }

        if (items.isEmpty()) {
            return null;
        }

        BigDecimal shippingFee = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(15_000, 50_001));
        return new OrderDraft(shop, items, subtotal, shippingFee, productsByItemId);
    }

    private Order createOrder(
            User user,
            OrderDraft draft,
            OrderStatus status,
            PaymentMethod paymentMethod,
            LocalDateTime createdAt,
            CouponSelection couponSelection,
            boolean hasCart
    ) {
        BigDecimal discountAmount = couponSelection.discountAmount();
        BigDecimal totalAmount = draft.subtotal().add(draft.shippingFee()).subtract(discountAmount);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        boolean isPaid = status == OrderStatus.DELIVERED || (status == OrderStatus.SHIPPING && paymentMethod == PaymentMethod.VNPAY);

        Order order = Order.builder()
                .orderCode(generateUniqueOrderCode())
                .userUuid(java.util.UUID.fromString(user.getId()))
                .shopUuid(java.util.UUID.fromString(draft.shop().getId()))
                .shopName(draft.shop().getShopName())
                .shippingAddress(toShippingAddress(user.getAddress()))
                .subtotalDecimal(draft.subtotal())
                .shippingFeeDecimal(draft.shippingFee())
                .discountAmountDecimal(discountAmount)
                .totalAmountDecimal(totalAmount)
                .couponCode(couponSelection.couponCode())
                .userCouponId(couponSelection.userCouponId())
                .paymentMethod(paymentMethod)
                .isPaid(isPaid)
                .paidAt(isPaid ? createdAt.plusHours(ThreadLocalRandom.current().nextInt(1, 24)) : null)
                .status(status)
                .statusHistory(buildStatusHistory(user.getId(), status, createdAt))
                .isFromCart(hasCart && ThreadLocalRandom.current().nextDouble() < 0.45)
                .createdAt(createdAt)
                .updatedAt(createdAt.plusHours(ThreadLocalRandom.current().nextInt(1, 12)))
                .build();

        applyLifecycleTimestamps(order, status, createdAt, paymentMethod);

        for (OrderItem item : draft.items()) {
            item.setOrder(order);
        }
        order.setItems(draft.items());

        return order;
    }

    private void applyLifecycleTimestamps(Order order, OrderStatus status, LocalDateTime createdAt, PaymentMethod paymentMethod) {
        if (status == OrderStatus.CONFIRMED || status == OrderStatus.SHIPPING || status == OrderStatus.DELIVERED) {
            order.setConfirmedAt(createdAt.plusHours(ThreadLocalRandom.current().nextInt(2, 25)));
        }

        if (status == OrderStatus.SHIPPING || status == OrderStatus.DELIVERED) {
            LocalDateTime shippingAt = createdAt.plusDays(ThreadLocalRandom.current().nextInt(1, 4))
                    .plusHours(ThreadLocalRandom.current().nextInt(1, 10));
            order.setShippingAt(shippingAt);

            if (status == OrderStatus.SHIPPING && paymentMethod == PaymentMethod.VNPAY) {
                order.setPaidAt(shippingAt);
            }
        }

        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(createdAt.plusDays(ThreadLocalRandom.current().nextInt(3, 11)));
        }

        if (status == OrderStatus.CANCELLED) {
            order.setCancelledAt(createdAt.plusHours(ThreadLocalRandom.current().nextInt(1, 49)));
            order.setCancelReason("Khach hang huy don");
        }
    }

    private CouponSelection selectCoupon(
            List<UserCoupon> activeCoupons,
            List<OrderItem> items,
            Map<String, Product> productsByItemId,
            LocalDateTime createdAt,
            double subtotal,
            double shippingFee
    ) {
        boolean applyCoupon = ThreadLocalRandom.current().nextDouble() < 0.3;
        if (!applyCoupon || activeCoupons == null || activeCoupons.isEmpty()) {
            return CouponSelection.none();
        }

        List<UserCoupon> applicableCoupons = activeCoupons.stream()
                .filter(Objects::nonNull)
                .filter(coupon -> isCouponApplicable(coupon, items, productsByItemId, createdAt, subtotal))
                .toList();

        if (applicableCoupons.isEmpty()) {
            return CouponSelection.none();
        }

        UserCoupon selectedCoupon = applicableCoupons.get(ThreadLocalRandom.current().nextInt(applicableCoupons.size()));
        double discount = calculateDiscount(selectedCoupon, subtotal, shippingFee);

        return new CouponSelection(
                selectedCoupon.getId(),
                selectedCoupon.getCode(),
                BigDecimal.valueOf(Math.max(discount, 0.0))
        );
    }

    private boolean isCouponApplicable(
            UserCoupon coupon,
            List<OrderItem> items,
            Map<String, Product> productsByItemId,
            LocalDateTime createdAt,
            double subtotal
    ) {
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            return false;
        }

        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(createdAt)) {
            return false;
        }

        if (coupon.getMinOrderAmount() != null && subtotal < coupon.getMinOrderAmount()) {
            return false;
        }

        if (coupon.getApplicableProductIds() != null && !coupon.getApplicableProductIds().isEmpty()) {
            boolean hasMatchedProduct = items.stream()
                    .map(OrderItem::getProductId)
                    .anyMatch(productId -> coupon.getApplicableProductIds().contains(productId));
            if (!hasMatchedProduct) {
                return false;
            }
        }

        if (coupon.getApplicableCategoryIds() != null && !coupon.getApplicableCategoryIds().isEmpty()) {
            boolean hasMatchedCategory = items.stream()
                    .map(OrderItem::getProductId)
                    .map(productsByItemId::get)
                    .filter(Objects::nonNull)
                    .map(Product::getCategoryId)
                    .anyMatch(categoryId -> coupon.getApplicableCategoryIds().contains(categoryId));
            if (!hasMatchedCategory) {
                return false;
            }
        }

        return true;
    }

    private double calculateDiscount(UserCoupon coupon, double subtotal, double shippingFee) {
        if (coupon.getCouponType() == null || coupon.getDiscountValue() == null) {
            return 0.0;
        }

        return switch (coupon.getCouponType()) {
            case PERCENTAGE -> subtotal * coupon.getDiscountValue() / 100.0;
            case FIXED -> Math.min(coupon.getDiscountValue(), subtotal);
            case FREE_SHIPPING -> shippingFee;
        };
    }

    private Product selectUniqueProduct(List<Product> shopProducts, String preferredCategory, Set<String> chosenProductIds) {
        int retries = Math.max(3, shopProducts.size());
        for (int i = 0; i < retries; i++) {
            Product candidate = productPopularityTracker.selectProduct(shopProducts, preferredCategory);
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            if (chosenProductIds.add(candidate.getId())) {
                return candidate;
            }
        }

        for (Product product : shopProducts) {
            if (product != null && product.getId() != null && chosenProductIds.add(product.getId())) {
                return product;
            }
        }

        return null;
    }

    private LocalDateTime generateRecentWeightedCreatedAt() {
        double u = ThreadLocalRandom.current().nextDouble(0.0001, 0.9999);
        double lambda = 0.35;
        double monthsAgo = Math.min(12.0, -Math.log(1 - u) / lambda);

        long daysAgo = Math.max(0, Math.round(monthsAgo * 30));
        return LocalDateTime.now()
                .minusDays(daysAgo)
                .minusHours(ThreadLocalRandom.current().nextInt(0, 24))
                .minusMinutes(ThreadLocalRandom.current().nextInt(0, 60));
    }

    private PaymentMethod resolvePaymentMethod() {
        return ThreadLocalRandom.current().nextDouble() < 0.7 ? PaymentMethod.COD : PaymentMethod.VNPAY;
    }

    private OrderStatus resolveOrderStatus() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 60) {
            return OrderStatus.DELIVERED;
        }
        if (roll < 75) {
            return OrderStatus.CANCELLED;
        }
        if (roll < 85) {
            return OrderStatus.SHIPPING;
        }
        if (roll < 95) {
            return OrderStatus.CONFIRMED;
        }
        return OrderStatus.PENDING;
    }

    private String pickPreferredCategory(List<String> preferredCategories) {
        if (preferredCategories == null || preferredCategories.isEmpty()) {
            return null;
        }

        if (preferredCategories.size() == 1) {
            return preferredCategories.get(0);
        }

        if (ThreadLocalRandom.current().nextDouble() < 0.7) {
            return preferredCategories.get(0);
        }

        int secondaryIndex = ThreadLocalRandom.current().nextInt(1, preferredCategories.size());
        return preferredCategories.get(secondaryIndex);
    }

    private String generateUniqueOrderCode() {
        for (int i = 0; i < 6; i++) {
            String orderCode = generateOrderCode();
            if (orderRepository.findByOrderCode(orderCode).isEmpty()) {
                return orderCode;
            }
        }

        return generateOrderCode() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String generateOrderCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return String.format("ORD%s-%04d", timestamp, suffix);
    }

    private Order.ShippingAddress toShippingAddress(User.Address address) {
        if (address == null) {
            return null;
        }

        return Order.ShippingAddress.builder()
                .provinceCode(address.getProvinceCode())
                .provinceName(address.getProvinceName())
                .communeCode(address.getCommuneCode())
                .communeName(address.getCommuneName())
                .detailAddress(address.getDetailAddress())
                .fullAddress(address.getFullAddress())
                .build();
    }

    private List<Order.StatusHistory> buildStatusHistory(String userId, OrderStatus finalStatus, LocalDateTime createdAt) {
        List<Order.StatusHistory> history = new ArrayList<>();
        history.add(Order.StatusHistory.builder()
                .status(OrderStatus.PENDING)
                .note("Don hang duoc tao")
                .updatedBy(userId)
                .updatedAt(createdAt)
                .build());

        if (finalStatus != OrderStatus.PENDING) {
            history.add(Order.StatusHistory.builder()
                    .status(finalStatus)
                    .note("Cap nhat trang thai: " + finalStatus)
                    .updatedBy(userId)
                    .updatedAt(createdAt.plusHours(ThreadLocalRandom.current().nextInt(1, 24)))
                    .build());
        }

        return history;
    }

    private List<Product> resolveProducts(List<Product> inputProducts) {
        if (inputProducts != null && !inputProducts.isEmpty()) {
            return inputProducts.stream()
                    .filter(Objects::nonNull)
                    .filter(product -> product.getId() != null)
                    .toList();
        }

        return productRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(product -> product.getId() != null)
                .toList();
    }

    private List<Shop> resolveShops(List<Shop> inputShops) {
        if (inputShops != null && !inputShops.isEmpty()) {
            return inputShops.stream()
                    .filter(Objects::nonNull)
                    .filter(shop -> shop.getId() != null)
                    .toList();
        }

        return shopRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(shop -> shop.getId() != null)
                .toList();
    }

    private double resolveProductPrice(Product product) {
        if (product.getFinalPrice() != null && product.getFinalPrice() > 0) {
            return product.getFinalPrice();
        }
        return product.getPrice() == null ? 0.0 : product.getPrice();
    }

    private String resolveProductImage(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().get(0);
    }

    private record OrderDraft(
            Shop shop,
            List<OrderItem> items,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            Map<String, Product> productsByItemId
    ) {
    }

    private record CouponSelection(
            String userCouponId,
            String couponCode,
            BigDecimal discountAmount
    ) {
        private static CouponSelection none() {
            return new CouponSelection(null, null, BigDecimal.ZERO);
        }
    }
}
