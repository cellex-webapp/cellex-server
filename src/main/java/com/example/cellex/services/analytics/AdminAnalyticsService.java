package com.example.cellex.services.analytics;

import com.example.cellex.dtos.response.analytics.*;
import com.example.cellex.dtos.response.analytics.AdminMainDashboardResponse.*;
import com.example.cellex.dtos.response.analytics.ChartDataPoint.*;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.Role;
import com.example.cellex.enums.ShopStatus;
import com.example.cellex.models.category.Category;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.category.CategoryRepository;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý Business Analytics & Reporting
 * Cung cấp các số liệu chuyên nghiệp cho Admin Dashboard
 * 
 * Endpoints:
 * - Admin Main Dashboard: Tổng quan hệ thống
 * - Customer Analytics: Chi tiết về khách hàng
 * - Product Analytics: Chi tiết về sản phẩm
 * - Shop Analytics: Chi tiết về cửa hàng
 * - Vendor Dashboard: Thống kê cho từng cửa hàng
 * 
 * @author Cellex Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    // ==================== ADMIN MAIN DASHBOARD ====================

    /**
     * Lấy Admin Main Dashboard với time filter
     * 
     * @param startDate Ngày bắt đầu (optional, default: đầu tháng)
     * @param endDate Ngày kết thúc (optional, default: hôm nay)
     * @return AdminMainDashboardResponse
     */
    public AdminMainDashboardResponse getAdminDashboard(LocalDate startDate, LocalDate endDate) {
        // Set default date range
        DateRangeInfo dateRange = calculateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = dateRange.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = dateRange.getEndDate().atTime(LocalTime.MAX);
        LocalDateTime prevStartDateTime = dateRange.getPreviousStartDate().atStartOfDay();
        LocalDateTime prevEndDateTime = dateRange.getPreviousEndDate().atTime(LocalTime.MAX);

        // Build Summary Cards (3 metrics chính)
        List<DashboardSummaryCard> summaryCards = buildMainSummaryCards(
                startDateTime, endDateTime, prevStartDateTime, prevEndDateTime);

        // Build Secondary KPIs
        SecondaryKPIs secondaryKPIs = buildSecondaryKPIs(
                startDateTime, endDateTime, prevStartDateTime, prevEndDateTime);

        // Build Charts
        DashboardCharts charts = buildDashboardCharts(dateRange.getStartDate(), dateRange.getEndDate());

        // Build Top Performers
        TopPerformers topPerformers = buildTopPerformers(startDateTime, endDateTime);

        // Build Recent Activities
        RecentActivities recentActivities = buildRecentActivities();

        return AdminMainDashboardResponse.builder()
                .dateRange(dateRange)
                .summaryCards(summaryCards)
                .secondaryKPIs(secondaryKPIs)
                .charts(charts)
                .topPerformers(topPerformers)
                .recentActivities(recentActivities)
                .build();
    }

    /**
     * Build 3 Summary Cards chính cho Dashboard
     */
    private List<DashboardSummaryCard> buildMainSummaryCards(
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            LocalDateTime prevStartDateTime, LocalDateTime prevEndDateTime) {
        
        List<DashboardSummaryCard> cards = new ArrayList<>();

        // Card 1: Tổng doanh thu
        double currentRevenue = calculateSystemRevenue(startDateTime, endDateTime);
        double previousRevenue = calculateSystemRevenue(prevStartDateTime, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Tổng doanh thu",
                currentRevenue,
                previousRevenue,
                "VND",
                DashboardSummaryCard.MetricType.CURRENCY,
                "currency-dollar",
                "Doanh thu từ đơn hàng hoàn thành và đã thanh toán"
        ));

        // Card 2: Tổng đơn hàng
        long currentOrders = orderRepository.countOrdersByDateRange(startDateTime, endDateTime);
        long previousOrders = orderRepository.countOrdersByDateRange(prevStartDateTime, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Tổng đơn hàng",
                (double) currentOrders,
                (double) previousOrders,
                "đơn",
                DashboardSummaryCard.MetricType.NUMBER,
                "shopping-cart",
                "Tổng số đơn hàng trong kỳ"
        ));

        // Card 3: Khách hàng mới
        long currentNewUsers = userRepository.countByCreatedAtBetween(startDateTime, endDateTime);
        long previousNewUsers = userRepository.countByCreatedAtBetween(prevStartDateTime, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Khách hàng mới",
                (double) currentNewUsers,
                (double) previousNewUsers,
                "người",
                DashboardSummaryCard.MetricType.NUMBER,
                "users",
                "Số khách hàng đăng ký mới"
        ));

        return cards;
    }

    /**
     * Build Secondary KPIs
     */
    private SecondaryKPIs buildSecondaryKPIs(
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            LocalDateTime prevStartDateTime, LocalDateTime prevEndDateTime) {
        
        // AOV - Average Order Value
        double currentRevenue = calculateSystemRevenue(startDateTime, endDateTime);
        long currentOrders = orderRepository.countOrdersByDateRange(startDateTime, endDateTime);
        double currentAOV = currentOrders > 0 ? currentRevenue / currentOrders : 0;

        double prevRevenue = calculateSystemRevenue(prevStartDateTime, prevEndDateTime);
        long prevOrders = orderRepository.countOrdersByDateRange(prevStartDateTime, prevEndDateTime);
        double prevAOV = prevOrders > 0 ? prevRevenue / prevOrders : 0;
        double aovChange = prevAOV > 0 ? ((currentAOV - prevAOV) / prevAOV) * 100 : 0;

        // Conversion Rate (DELIVERED / Total)
        long deliveredOrders = getCompletedOrdersCount(startDateTime, endDateTime);
        double conversionRate = currentOrders > 0 ? (double) deliveredOrders / currentOrders * 100 : 0;
        
        long prevDeliveredOrders = getCompletedOrdersCount(prevStartDateTime, prevEndDateTime);
        double prevConversionRate = prevOrders > 0 ? (double) prevDeliveredOrders / prevOrders * 100 : 0;
        double conversionRateChange = prevConversionRate > 0 ? 
                ((conversionRate - prevConversionRate) / prevConversionRate) * 100 : 0;

        // Cancellation Rate
        long cancelledOrders = orderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.CANCELLED, startDateTime, endDateTime);
        double cancellationRate = currentOrders > 0 ? (double) cancelledOrders / currentOrders * 100 : 0;

        long prevCancelledOrders = orderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.CANCELLED, prevStartDateTime, prevEndDateTime);
        double prevCancellationRate = prevOrders > 0 ? (double) prevCancelledOrders / prevOrders * 100 : 0;
        double cancellationRateChange = prevCancellationRate > 0 ? 
                ((cancellationRate - prevCancellationRate) / prevCancellationRate) * 100 : 0;

        // Active Shops
        long activeShops = shopRepository.countByStatus(ShopStatus.APPROVED);
        long newShops = shopRepository.countByStatusAndCreatedAtBetween(
                ShopStatus.APPROVED, startDateTime, endDateTime);

        // Active Products
        long activeProducts = productRepository.countByIsPublishedTrue();
        long newProducts = productRepository.countByIsPublishedTrueAndCreatedAtBetween(
                startDateTime, endDateTime);

        return SecondaryKPIs.builder()
                .averageOrderValue(Math.round(currentAOV * 100.0) / 100.0)
                .aovChange(Math.round(aovChange * 100.0) / 100.0)
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .conversionRateChange(Math.round(conversionRateChange * 100.0) / 100.0)
                .cancellationRate(Math.round(cancellationRate * 100.0) / 100.0)
                .cancellationRateChange(Math.round(cancellationRateChange * 100.0) / 100.0)
                .activeShops(activeShops)
                .newShopsThisPeriod(newShops)
                .activeProducts(activeProducts)
                .newProductsThisPeriod(newProducts)
                .build();
    }

    /**
     * Build Dashboard Charts
     */
    private DashboardCharts buildDashboardCharts(LocalDate startDate, LocalDate endDate) {
        return DashboardCharts.builder()
                .revenueChart(buildRevenueTimeSeriesChart(startDate, endDate))
                .ordersChart(buildOrdersTimeSeriesChart(startDate, endDate))
                .orderStatusDistribution(buildOrderStatusPieChart())
                .revenueByCategoryChart(buildRevenueByCategoryChart(startDate, endDate))
                .build();
    }

    /**
     * Build Revenue Time Series Chart
     */
    private TimeSeriesChart buildRevenueTimeSeriesChart(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        double total = 0;

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        DateTimeFormatter formatter = daysBetween > 31 ? 
                DateTimeFormatter.ofPattern("dd/MM") : DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            double revenue = calculateSystemRevenue(
                    date.atStartOfDay(), 
                    date.atTime(LocalTime.MAX));
            
            dataPoints.add(ChartDataPoint.builder()
                    .label(date.format(formatter))
                    .value(revenue)
                    .date(date)
                    .build());
            total += revenue;
        }

        return TimeSeriesChart.builder()
                .title("Doanh thu theo ngày")
                .chartType(ChartType.AREA)
                .data(dataPoints)
                .xAxisLabel("Ngày")
                .yAxisLabel("Doanh thu (VND)")
                .unit("VND")
                .total(total)
                .average(dataPoints.isEmpty() ? 0 : total / dataPoints.size())
                .build();
    }

    /**
     * Build Orders Time Series Chart
     */
    private TimeSeriesChart buildOrdersTimeSeriesChart(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        long total = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long orders = orderRepository.countOrdersByDateRange(
                    date.atStartOfDay(), 
                    date.atTime(LocalTime.MAX));
            
            dataPoints.add(ChartDataPoint.builder()
                    .label(date.format(formatter))
                    .value((double) orders)
                    .date(date)
                    .build());
            total += orders;
        }

        return TimeSeriesChart.builder()
                .title("Số đơn hàng theo ngày")
                .chartType(ChartType.BAR)
                .data(dataPoints)
                .xAxisLabel("Ngày")
                .yAxisLabel("Số đơn")
                .unit("đơn")
                .total((double) total)
                .average(dataPoints.isEmpty() ? 0 : (double) total / dataPoints.size())
                .build();
    }

    /**
     * Build Order Status Pie Chart
     */
    private PieChartData buildOrderStatusPieChart() {
        List<PieSlice> slices = new ArrayList<>();
        long total = orderRepository.count();

        Map<OrderStatus, String> statusColors = Map.of(
                OrderStatus.PENDING, "#FFB020",
                OrderStatus.CONFIRMED, "#3366FF",
                OrderStatus.SHIPPING, "#00B8D9",
                OrderStatus.DELIVERED, "#36B37E",
                OrderStatus.CANCELLED, "#FF5630"
        );

        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            if (count > 0) {
                slices.add(PieSlice.builder()
                        .label(getStatusDisplayName(status))
                        .value((double) count)
                        .percentage(total > 0 ? Math.round((double) count / total * 10000.0) / 100.0 : 0)
                        .color(statusColors.getOrDefault(status, "#8B8D97"))
                        .build());
            }
        }

        return PieChartData.builder()
                .title("Phân bổ trạng thái đơn hàng")
                .slices(slices)
                .total((double) total)
                .build();
    }

    /**
     * Build Revenue by Category Pie Chart
     */
    private PieChartData buildRevenueByCategoryChart(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByDateRange(
                startDateTime, endDateTime);

        // Aggregate revenue by category (lookup from product)
        Map<String, Double> revenueByCategory = new HashMap<>();
        for (Order order : completedOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    // Skip items with null productId
                    if (item.getProductId() == null) {
                        log.warn("OrderItem with null productId found in order: {}", order.getId());
                        continue;
                    }
                    // Get category from product
                    String categoryId = productRepository.findById(item.getProductId())
                            .map(Product::getCategoryId)
                            .orElse(null);
                    if (categoryId != null) {
                        revenueByCategory.merge(categoryId, item.getSubtotal(), Double::sum);
                    }
                }
            }
        }

        // Get category names and build slices
        List<PieSlice> slices = new ArrayList<>();
        double total = revenueByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
        
        String[] colors = {"#3366FF", "#00B8D9", "#36B37E", "#FFB020", "#FF5630", "#8B8D97"};
        int colorIndex = 0;

        for (Map.Entry<String, Double> entry : revenueByCategory.entrySet()) {
            String categoryName = getCategoryName(entry.getKey());
            slices.add(PieSlice.builder()
                    .label(categoryName)
                    .value(entry.getValue())
                    .percentage(total > 0 ? Math.round(entry.getValue() / total * 10000.0) / 100.0 : 0)
                    .color(colors[colorIndex % colors.length])
                    .build());
            colorIndex++;
        }

        // Sort by value descending and limit to top 5 + Others
        slices.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        if (slices.size() > 5) {
            double othersValue = slices.subList(5, slices.size()).stream()
                    .mapToDouble(PieSlice::getValue).sum();
            slices = new ArrayList<>(slices.subList(0, 5));
            slices.add(PieSlice.builder()
                    .label("Khác")
                    .value(othersValue)
                    .percentage(total > 0 ? Math.round(othersValue / total * 10000.0) / 100.0 : 0)
                    .color("#8B8D97")
                    .build());
        }

        return PieChartData.builder()
                .title("Doanh thu theo danh mục")
                .slices(slices)
                .total(total)
                .build();
    }

    /**
     * Build Top Performers
     */
    private TopPerformers buildTopPerformers(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return TopPerformers.builder()
                .topShops(getTopShopsByRevenue(startDateTime, endDateTime, 5))
                .topProducts(getTopProductsByRevenue(startDateTime, endDateTime, 5))
                .topCustomers(getTopCustomersBySpending(startDateTime, endDateTime, 5))
                .build();
    }

    /**
     * Get Top Shops by Revenue
     */
    private List<TopShopItem> getTopShopsByRevenue(LocalDateTime start, LocalDateTime end, int limit) {
        List<Shop> activeShops = shopRepository.findByStatus(ShopStatus.APPROVED);
        
        List<ShopRevenueData> shopRevenueList = activeShops.stream()
                .map(shop -> {
                    List<Order> orders = orderRepository.findCompletedPaidOrdersByShopIdAndDateRange(
                            shop.getId(), start, end);
                    double revenue = orders.stream().mapToDouble(Order::getTotalAmount).sum();
                    return new ShopRevenueData(shop, revenue, (long) orders.size());
                })
                .sorted((a, b) -> Double.compare(b.revenue, a.revenue))
                .limit(limit)
                .collect(Collectors.toList());

        List<TopShopItem> result = new ArrayList<>();
        for (int i = 0; i < shopRevenueList.size(); i++) {
            ShopRevenueData data = shopRevenueList.get(i);
            result.add(TopShopItem.builder()
                    .rank(i + 1)
                    .shopId(data.shop.getId())
                    .shopName(data.shop.getShopName())
                    .logoUrl(data.shop.getLogoUrl())
                    .revenue(data.revenue)
                    .orderCount(data.orderCount)
                    .rating(data.shop.getRating())
                    .build());
        }
        return result;
    }

    /**
     * Get Top Products by Revenue
     */
    private List<AdminMainDashboardResponse.TopProductItem> getTopProductsByRevenue(
            LocalDateTime start, LocalDateTime end, int limit) {
        
        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        
        // Aggregate by product
        Map<String, ProductSalesData> salesByProduct = new HashMap<>();
        for (Order order : completedOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    String productId = item.getProductId();
                    ProductSalesData data = salesByProduct.getOrDefault(productId,
                            new ProductSalesData(productId, item.getProductName(), 
                                    item.getProductImage(), order.getShopName(), 0L, 0.0));
                    data.totalQuantity += item.getQuantity();
                    data.totalRevenue += item.getSubtotal();
                    salesByProduct.put(productId, data);
                }
            }
        }

        List<AdminMainDashboardResponse.TopProductItem> result = salesByProduct.values().stream()
                .sorted((a, b) -> Double.compare(b.totalRevenue, a.totalRevenue))
                .limit(limit)
                .map(data -> AdminMainDashboardResponse.TopProductItem.builder()
                        .productId(data.productId)
                        .productName(data.productName)
                        .imageUrl(data.productImage)
                        .shopName(data.shopName)
                        .quantitySold(data.totalQuantity)
                        .revenue(data.totalRevenue)
                        .build())
                .collect(Collectors.toList());

        // Add ranks
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }
        return result;
    }

    /**
     * Get Top Customers by Spending
     */
    private List<TopCustomerItem> getTopCustomersBySpending(
            LocalDateTime start, LocalDateTime end, int limit) {
        
        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        
        // Aggregate by user
        Map<String, CustomerSpendingData> spendingByUser = new HashMap<>();
        for (Order order : completedOrders) {
            String userId = order.getUserId();
            CustomerSpendingData data = spendingByUser.getOrDefault(userId,
                    new CustomerSpendingData(userId, 0.0, 0L, order.getCreatedAt()));
            data.totalSpent += order.getTotalAmount();
            data.orderCount++;
            if (order.getCreatedAt() != null && 
                    (data.lastOrderDate == null || order.getCreatedAt().isAfter(data.lastOrderDate))) {
                data.lastOrderDate = order.getCreatedAt();
            }
            spendingByUser.put(userId, data);
        }

        List<TopCustomerItem> result = spendingByUser.values().stream()
                .sorted((a, b) -> Double.compare(b.totalSpent, a.totalSpent))
                .limit(limit)
                .map(data -> {
                    User user = userRepository.findById(data.userId).orElse(null);
                    return TopCustomerItem.builder()
                            .userId(data.userId)
                            .fullName(user != null ? user.getFullName() : "Unknown")
                            .avatarUrl(user != null ? user.getAvatarUrl() : null)
                            .totalSpent(data.totalSpent)
                            .orderCount(data.orderCount)
                            .lastOrderDate(data.lastOrderDate)
                            .build();
                })
                .collect(Collectors.toList());

        // Add ranks
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }
        return result;
    }

    /**
     * Build Recent Activities
     */
    private RecentActivities buildRecentActivities() {
        return RecentActivities.builder()
                .recentOrders(getRecentOrders(5))
                .newShops(getRecentShops(5))
                .newUsers(getRecentUsers(5))
                .build();
    }

    /**
     * Get Recent Orders
     */
    private List<RecentOrderItem> getRecentOrders(int limit) {
        List<Order> orders = orderRepository.findTop5ByOrderByCreatedAtDesc();
        
        return orders.stream()
                .limit(limit)
                .map(order -> {
                    User user = userRepository.findById(order.getUserId()).orElse(null);
                    return RecentOrderItem.builder()
                            .orderId(order.getId())
                            .customerName(user != null ? user.getFullName() : "Unknown")
                            .shopName(order.getShopName())
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
     * Get Recent Shops
     */
    private List<RecentShopItem> getRecentShops(int limit) {
        List<Shop> shops = shopRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        
        return shops.stream()
                .map(shop -> {
                    User vendor = userRepository.findById(shop.getVendorId()).orElse(null);
                    return RecentShopItem.builder()
                            .shopId(shop.getId())
                            .shopName(shop.getShopName())
                            .logoUrl(shop.getLogoUrl())
                            .vendorName(vendor != null ? vendor.getFullName() : "Unknown")
                            .status(shop.getStatus().name())
                            .createdAt(shop.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get Recent Users
     */
    private List<RecentUserItem> getRecentUsers(int limit) {
        List<User> users = userRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        
        return users.stream()
                .filter(u -> u.getRole() == Role.USER)
                .limit(limit)
                .map(user -> RecentUserItem.builder()
                        .userId(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Calculate date range with previous period for comparison
     */
    private DateRangeInfo calculateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Ensure startDate <= endDate
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        // Calculate previous period
        long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(periodDays - 1);

        // Determine period type
        String period = determinePeriodType(startDate, endDate);

        return DateRangeInfo.builder()
                .startDate(startDate)
                .endDate(endDate)
                .period(period)
                .previousStartDate(prevStartDate)
                .previousEndDate(prevEndDate)
                .build();
    }

    private String determinePeriodType(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        
        if (startDate.equals(today) && endDate.equals(today)) {
            return "TODAY";
        }
        if (startDate.equals(today.with(DayOfWeek.MONDAY)) && endDate.equals(today)) {
            return "THIS_WEEK";
        }
        if (startDate.equals(today.withDayOfMonth(1)) && endDate.equals(today)) {
            return "THIS_MONTH";
        }
        if (startDate.equals(today.withDayOfYear(1)) && endDate.equals(today)) {
            return "THIS_YEAR";
        }
        return "CUSTOM";
    }

    /**
     * Calculate system revenue for date range
     */
    private double calculateSystemRevenue(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Order> orders = orderRepository.findCompletedPaidOrdersByDateRange(startDateTime, endDateTime);
        return orders.stream().mapToDouble(Order::getTotalAmount).sum();
    }

    /**
     * Get completed orders count for date range
     */
    private long getCompletedOrdersCount(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return orderRepository.countCompletedPaidOrdersByDateRange(startDateTime, endDateTime);
    }

    /**
     * Get status display name
     */
    private String getStatusDisplayName(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case READY_TO_SHIP -> "Chờ lấy hàng";
            case SHIPPING -> "Đang vận chuyển";
            case DELIVERED -> "Đã giao";
            case CANCELLED -> "Đã hủy";
            case DELIVERY_FAILED -> "Giao thất bại";
            case RETURNING -> "Đang hoàn trả";
            case RETURNED -> "Đã hoàn trả";
        };
    }

    /**
     * Get category name by ID
     */
    private String getCategoryName(String categoryId) {
        if (categoryId == null) return "Không phân loại";
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse("Không phân loại");
    }

    // ==================== HELPER CLASSES ====================

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

    private static class ProductSalesData {
        String productId;
        String productName;
        String productImage;
        String shopName;
        Long totalQuantity;
        Double totalRevenue;

        ProductSalesData(String productId, String productName, String productImage, 
                        String shopName, Long totalQuantity, Double totalRevenue) {
            this.productId = productId;
            this.productName = productName;
            this.productImage = productImage;
            this.shopName = shopName;
            this.totalQuantity = totalQuantity;
            this.totalRevenue = totalRevenue;
        }
    }

    private static class CustomerSpendingData {
        String userId;
        Double totalSpent;
        Long orderCount;
        LocalDateTime lastOrderDate;

        CustomerSpendingData(String userId, Double totalSpent, Long orderCount, LocalDateTime lastOrderDate) {
            this.userId = userId;
            this.totalSpent = totalSpent;
            this.orderCount = orderCount;
            this.lastOrderDate = lastOrderDate;
        }
    }
}
