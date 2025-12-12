package com.example.cellex.services.analytics;

import com.example.cellex.dtos.response.analytics.AdminDashboardResponse;
import com.example.cellex.dtos.response.analytics.AdminDashboardResponse.*;
import com.example.cellex.dtos.response.analytics.VendorDashboardResponse;
import com.example.cellex.dtos.response.analytics.VendorDashboardResponse.*;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.Role;
import com.example.cellex.enums.ShopStatus;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý Business Analytics & Reporting
 * 
 * Bao gồm:
 * - Vendor Dashboard: Thống kê cho từng cửa hàng
 * - Admin Dashboard: Thống kê toàn hệ thống
 * 
 * Business Rules:
 * - BR-VD-03-04: Chỉ tính doanh thu từ đơn hàng DELIVERED và đã thanh toán (isPaid = true)
 * - Vendor chỉ được xem dữ liệu của shop mình sở hữu
 * 
 * @author Cellex Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ==================== VENDOR DASHBOARD ====================

    /**
     * Lấy dashboard đầy đủ cho Vendor
     *
     * @param shopId ID của shop
     * @param currentUser User hiện tại (để validate quyền truy cập)
     * @param startDate Ngày bắt đầu (optional)
     * @param endDate Ngày kết thúc (optional)
     * @return VendorDashboardResponse
     */
    public VendorDashboardResponse getVendorDashboard(String shopId, User currentUser, 
                                                       LocalDate startDate, LocalDate endDate) {
        // Validate quyền truy cập
        validateVendorAccess(shopId, currentUser);

        // Lấy thông tin shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Set default date range nếu không có
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1); // Đầu tháng hiện tại
        }
        if (endDate == null) {
            endDate = LocalDate.now(); // Hôm nay
        }

        return VendorDashboardResponse.builder()
                .shopInfo(buildShopInfo(shop))
                .revenueStats(getVendorRevenue(shopId, startDate, endDate))
                .orderStatistics(getOrderStatistics(shopId))
                .bestSellingProducts(getBestSellingProducts(shopId))
                .recentOrders(getVendorRecentOrders(shopId))
                .build();
    }

    /**
     * Validate quyền truy cập của vendor vào shop
     * Vendor chỉ được xem dữ liệu của shop mình sở hữu
     *
     * @param shopId ID shop cần truy cập
     * @param currentUser User hiện tại
     * @throws AppException với MSG32 nếu không có quyền
     */
    private void validateVendorAccess(String shopId, User currentUser) {
        // Admin có quyền truy cập tất cả
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        // Vendor chỉ được truy cập shop của mình
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (!shop.getVendorId().equals(currentUser.getId())) {
            log.warn("Access denied: User {} tried to access shop {} data", 
                    currentUser.getId(), shopId);
            throw new AppException(ErrorCode.MSG32_ACCESS_DENIED);
        }
    }

    /**
     * Build thông tin shop
     */
    private ShopInfo buildShopInfo(Shop shop) {
        return ShopInfo.builder()
                .shopId(shop.getId())
                .shopName(shop.getShopName())
                .logoUrl(shop.getLogoUrl())
                .rating(shop.getRating())
                .build();
    }

    /**
     * Tính doanh thu của shop trong khoảng thời gian
     * Business Rule BR-VD-03-04: Chỉ tính đơn DELIVERED và đã thanh toán
     *
     * @param shopId ID shop
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return RevenueStats
     */
    public RevenueStats getVendorRevenue(String shopId, LocalDate startDate, LocalDate endDate) {
        // Validate date range
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        // Convert to LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Lấy đơn hàng đã hoàn thành và thanh toán
        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByShopIdAndDateRange(
                shopId, startDateTime, endDateTime);

        // Tính tổng doanh thu
        double totalRevenue = completedOrders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        // Tính giá trị đơn hàng trung bình
        double averageOrderValue = completedOrders.isEmpty() ? 0 : 
                totalRevenue / completedOrders.size();

        // Tính tăng trưởng so với kỳ trước
        Double growthPercent = calculateRevenueGrowth(shopId, startDate, endDate);

        return RevenueStats.builder()
                .totalRevenue(totalRevenue)
                .startDate(startDate)
                .endDate(endDate)
                .completedOrdersCount((long) completedOrders.size())
                .averageOrderValue(Math.round(averageOrderValue * 100.0) / 100.0)
                .revenueGrowthPercent(growthPercent)
                .build();
    }

    /**
     * Tính phần trăm tăng trưởng doanh thu so với kỳ trước
     */
    private Double calculateRevenueGrowth(String shopId, LocalDate startDate, LocalDate endDate) {
        // Tính độ dài kỳ hiện tại
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // Kỳ trước
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(periodDays - 1);

        LocalDateTime prevStartDateTime = prevStartDate.atStartOfDay();
        LocalDateTime prevEndDateTime = prevEndDate.atTime(LocalTime.MAX);

        // Doanh thu kỳ trước
        List<Order> prevOrders = orderRepository.findCompletedPaidOrdersByShopIdAndDateRange(
                shopId, prevStartDateTime, prevEndDateTime);
        double prevRevenue = prevOrders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        // Doanh thu kỳ hiện tại
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<Order> currentOrders = orderRepository.findCompletedPaidOrdersByShopIdAndDateRange(
                shopId, startDateTime, endDateTime);
        double currentRevenue = currentOrders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        // Tính phần trăm tăng trưởng
        if (prevRevenue == 0) {
            return currentRevenue > 0 ? 100.0 : 0.0;
        }

        double growth = ((currentRevenue - prevRevenue) / prevRevenue) * 100;
        return Math.round(growth * 100.0) / 100.0;
    }

    /**
     * Lấy top 5 sản phẩm bán chạy nhất của shop
     * Dựa trên số lượng đã bán trong các đơn hàng DELIVERED và đã thanh toán
     *
     * @param shopId ID shop
     * @return List<BestSellingProduct>
     */
    public List<BestSellingProduct> getBestSellingProducts(String shopId) {
        // Lấy tất cả đơn hàng đã hoàn thành
        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByShopId(shopId);

        // Aggregate số lượng bán theo product
        Map<String, ProductSalesData> salesByProduct = new HashMap<>();

        for (Order order : completedOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    String productId = item.getProductId();
                    ProductSalesData data = salesByProduct.getOrDefault(productId, 
                            new ProductSalesData(productId, item.getProductName(), 
                                    item.getProductImage(), 0L, 0.0));
                    
                    data.totalQuantity += item.getQuantity();
                    data.totalRevenue += item.getSubtotal();
                    
                    salesByProduct.put(productId, data);
                }
            }
        }

        // Sắp xếp và lấy top 5
        return salesByProduct.values().stream()
                .sorted((a, b) -> Long.compare(b.totalQuantity, a.totalQuantity))
                .limit(5)
                .map(data -> {
                    int rank = (int) salesByProduct.values().stream()
                            .filter(d -> d.totalQuantity > data.totalQuantity)
                            .count() + 1;
                    return BestSellingProduct.builder()
                            .productId(data.productId)
                            .productName(data.productName)
                            .productImage(data.productImage)
                            .totalQuantitySold(data.totalQuantity)
                            .totalRevenue(data.totalRevenue)
                            .rank(rank)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // Helper class cho việc aggregate sales data
    private static class ProductSalesData {
        String productId;
        String productName;
        String productImage;
        Long totalQuantity;
        Double totalRevenue;

        ProductSalesData(String productId, String productName, String productImage, 
                        Long totalQuantity, Double totalRevenue) {
            this.productId = productId;
            this.productName = productName;
            this.productImage = productImage;
            this.totalQuantity = totalQuantity;
            this.totalRevenue = totalRevenue;
        }
    }

    /**
     * Thống kê đơn hàng theo trạng thái của shop
     *
     * @param shopId ID shop
     * @return OrderStatistics
     */
    public OrderStatistics getOrderStatistics(String shopId) {
        long pending = orderRepository.countByShopIdAndStatus(shopId, OrderStatus.PENDING);
        long confirmed = orderRepository.countByShopIdAndStatus(shopId, OrderStatus.CONFIRMED);
        long shipping = orderRepository.countByShopIdAndStatus(shopId, OrderStatus.SHIPPING);
        long delivered = orderRepository.countByShopIdAndStatus(shopId, OrderStatus.DELIVERED);
        long cancelled = orderRepository.countByShopIdAndStatus(shopId, OrderStatus.CANCELLED);
        long total = orderRepository.countByShopId(shopId);

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        ordersByStatus.put("PENDING", pending);
        ordersByStatus.put("CONFIRMED", confirmed);
        ordersByStatus.put("SHIPPING", shipping);
        ordersByStatus.put("DELIVERED", delivered);
        ordersByStatus.put("CANCELLED", cancelled);

        return OrderStatistics.builder()
                .totalOrders(total)
                .pendingOrders(pending)
                .confirmedOrders(confirmed)
                .shippingOrders(shipping)
                .deliveredOrders(delivered)
                .cancelledOrders(cancelled)
                .ordersByStatus(ordersByStatus)
                .build();
    }

    /**
     * Lấy đơn hàng gần đây của shop
     */
    private List<RecentOrderInfo> getVendorRecentOrders(String shopId) {
        List<Order> recentOrders = orderRepository.findTop5ByShopIdOrderByCreatedAtDesc(shopId);
        
        return recentOrders.stream()
                .map(order -> {
                    User customer = userRepository.findById(order.getUserId()).orElse(null);
                    return RecentOrderInfo.builder()
                            .orderId(order.getId())
                            .customerName(customer != null ? customer.getFullName() : "Unknown")
                            .totalAmount(order.getTotalAmount())
                            .status(order.getStatus().name())
                            .createdAt(order.getCreatedAt() != null ? 
                                    order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== ADMIN DASHBOARD ====================

    /**
     * Lấy dashboard đầy đủ cho Admin
     *
     * @return AdminDashboardResponse
     */
    public AdminDashboardResponse getAdminDashboard() {
        return AdminDashboardResponse.builder()
                .systemOverview(getSystemOverview())
                .revenueStats(getSystemRevenueStats())
                .orderStats(getSystemOrderStats())
                .recentOrders(getRecentOrders())
                .topShops(getTopShops())
                .userStats(getUserStats())
                .build();
    }

    /**
     * Lấy tổng quan hệ thống
     *
     * @return SystemOverview
     */
    public SystemOverview getSystemOverview() {
        // Đếm shops
        long activeShops = shopRepository.countByStatus(ShopStatus.APPROVED);
        long pendingShops = shopRepository.countByStatus(ShopStatus.PENDING);

        // Đếm users
        long totalUsers = userRepository.count();

        // Đếm orders
        long totalOrders = orderRepository.count();

        // Tính tổng doanh thu (từ đơn DELIVERED và đã thanh toán)
        List<Order> completedOrders = orderRepository.findAllCompletedPaidOrders();
        double totalRevenue = completedOrders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        // Đếm products
        long totalProducts = productRepository.countByIsPublishedTrue();

        return SystemOverview.builder()
                .totalActiveShops(activeShops)
                .totalPendingShops(pendingShops)
                .totalUsers(totalUsers)
                .totalOrders(totalOrders)
                .totalSystemRevenue(totalRevenue)
                .totalProducts(totalProducts)
                .build();
    }

    /**
     * Lấy thống kê doanh thu hệ thống
     *
     * @return SystemRevenueStats
     */
    public SystemRevenueStats getSystemRevenueStats() {
        LocalDate today = LocalDate.now();
        
        // Doanh thu hôm nay
        double todayRevenue = calculateSystemRevenue(today, today);
        
        // Doanh thu tuần này (từ thứ 2)
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        double thisWeekRevenue = calculateSystemRevenue(startOfWeek, today);
        
        // Doanh thu tháng này
        LocalDate startOfMonth = today.withDayOfMonth(1);
        double thisMonthRevenue = calculateSystemRevenue(startOfMonth, today);
        
        // Doanh thu năm nay
        LocalDate startOfYear = today.withDayOfYear(1);
        double thisYearRevenue = calculateSystemRevenue(startOfYear, today);
        
        // Tăng trưởng so với tháng trước
        LocalDate lastMonthStart = startOfMonth.minusMonths(1);
        LocalDate lastMonthEnd = startOfMonth.minusDays(1);
        double lastMonthRevenue = calculateSystemRevenue(lastMonthStart, lastMonthEnd);
        
        double monthOverMonthGrowth = lastMonthRevenue == 0 ? 
                (thisMonthRevenue > 0 ? 100.0 : 0.0) :
                ((thisMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100;

        // Doanh thu theo ngày trong tuần hiện tại
        Map<String, Double> revenueByDay = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE dd/MM");
        for (int i = 0; i <= java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, today); i++) {
            LocalDate date = startOfWeek.plusDays(i);
            double revenue = calculateSystemRevenue(date, date);
            revenueByDay.put(date.format(dayFormatter), revenue);
        }

        return SystemRevenueStats.builder()
                .todayRevenue(todayRevenue)
                .thisWeekRevenue(thisWeekRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                .thisYearRevenue(thisYearRevenue)
                .monthOverMonthGrowth(Math.round(monthOverMonthGrowth * 100.0) / 100.0)
                .revenueByDay(revenueByDay)
                .build();
    }

    /**
     * Tính doanh thu hệ thống trong khoảng thời gian
     */
    private double calculateSystemRevenue(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Order> orders = orderRepository.findCompletedPaidOrdersByDateRange(startDateTime, endDateTime);
        return orders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }

    /**
     * Lấy thống kê đơn hàng toàn hệ thống
     *
     * @return SystemOrderStats
     */
    public SystemOrderStats getSystemOrderStats() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate startOfMonth = today.withDayOfMonth(1);

        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long shippingOrders = orderRepository.countByStatus(OrderStatus.SHIPPING);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        ordersByStatus.put("PENDING", pendingOrders);
        ordersByStatus.put("CONFIRMED", confirmedOrders);
        ordersByStatus.put("SHIPPING", shippingOrders);
        ordersByStatus.put("DELIVERED", deliveredOrders);
        ordersByStatus.put("CANCELLED", cancelledOrders);

        // Đơn hàng theo thời gian
        long todayOrders = orderRepository.countOrdersByDateRange(
                today.atStartOfDay(), today.atTime(LocalTime.MAX));
        long thisWeekOrders = orderRepository.countOrdersByDateRange(
                startOfWeek.atStartOfDay(), today.atTime(LocalTime.MAX));
        long thisMonthOrders = orderRepository.countOrdersByDateRange(
                startOfMonth.atStartOfDay(), today.atTime(LocalTime.MAX));

        // Tỷ lệ hủy đơn
        double cancellationRate = totalOrders == 0 ? 0 : 
                (double) cancelledOrders / totalOrders * 100;

        return SystemOrderStats.builder()
                .totalOrders(totalOrders)
                .ordersByStatus(ordersByStatus)
                .todayOrders(todayOrders)
                .thisWeekOrders(thisWeekOrders)
                .thisMonthOrders(thisMonthOrders)
                .cancellationRate(Math.round(cancellationRate * 100.0) / 100.0)
                .build();
    }

    /**
     * Lấy 5 đơn hàng gần đây nhất (toàn hệ thống)
     *
     * @return List<RecentOrder>
     */
    public List<RecentOrder> getRecentOrders() {
        List<Order> orders = orderRepository.findTop5ByOrderByCreatedAtDesc();
        
        return orders.stream()
                .map(order -> {
                    User user = userRepository.findById(order.getUserId()).orElse(null);
                    Shop shop = shopRepository.findById(order.getShopId()).orElse(null);
                    
                    return RecentOrder.builder()
                            .orderId(order.getId())
                            .userId(order.getUserId())
                            .userName(user != null ? user.getFullName() : "Unknown")
                            .shopId(order.getShopId())
                            .shopName(shop != null ? shop.getShopName() : order.getShopName())
                            .totalAmount(order.getTotalAmount())
                            .status(order.getStatus().name())
                            .paymentMethod(order.getPaymentMethod() != null ? 
                                    order.getPaymentMethod().name() : null)
                            .isPaid(order.getIsPaid())
                            .createdAt(order.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy top 5 cửa hàng có doanh thu cao nhất
     *
     * @return List<TopShop>
     */
    public List<TopShop> getTopShops() {
        // Lấy tất cả shop đang active
        List<Shop> activeShops = shopRepository.findByStatus(ShopStatus.APPROVED);
        
        // Tính doanh thu của từng shop
        List<ShopRevenueData> shopRevenueList = activeShops.stream()
                .map(shop -> {
                    List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByShopId(shop.getId());
                    double revenue = completedOrders.stream()
                            .mapToDouble(Order::getTotalAmount)
                            .sum();
                    return new ShopRevenueData(shop, revenue, (long) completedOrders.size());
                })
                .sorted((a, b) -> Double.compare(b.revenue, a.revenue))
                .limit(5)
                .collect(Collectors.toList());

        // Build response
        List<TopShop> result = new ArrayList<>();
        for (int i = 0; i < shopRevenueList.size(); i++) {
            ShopRevenueData data = shopRevenueList.get(i);
            result.add(TopShop.builder()
                    .shopId(data.shop.getId())
                    .shopName(data.shop.getShopName())
                    .logoUrl(data.shop.getLogoUrl())
                    .totalRevenue(data.revenue)
                    .totalCompletedOrders(data.orderCount)
                    .rank(i + 1)
                    .build());
        }
        
        return result;
    }

    // Helper class
    private static class ShopRevenueData {
        Shop shop;
        Double revenue;
        Long orderCount;

        ShopRevenueData(Shop shop, Double revenue, Long orderCount) {
            this.shop = shop;
            this.revenue = revenue;
            this.orderCount = orderCount;
        }
    }

    /**
     * Lấy thống kê người dùng
     *
     * @return UserStats
     */
    public UserStats getUserStats() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        long totalUsers = userRepository.count();
        long newUsersThisMonth = userRepository.countByCreatedAtBetween(
                startOfMonth.atStartOfDay(), today.atTime(LocalTime.MAX));
        long totalVendors = userRepository.countByRole(Role.VENDOR);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);
        long totalRegularUsers = userRepository.countByRole(Role.USER);

        Map<String, Long> usersByRole = new LinkedHashMap<>();
        usersByRole.put("USER", totalRegularUsers);
        usersByRole.put("VENDOR", totalVendors);
        usersByRole.put("ADMIN", totalAdmins);

        return UserStats.builder()
                .totalUsers(totalUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .totalVendors(totalVendors)
                .totalAdmins(totalAdmins)
                .usersByRole(usersByRole)
                .build();
    }
}
