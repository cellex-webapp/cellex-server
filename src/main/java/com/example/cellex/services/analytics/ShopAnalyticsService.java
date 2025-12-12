package com.example.cellex.services.analytics;

import com.example.cellex.dtos.response.analytics.*;
import com.example.cellex.dtos.response.analytics.AdminMainDashboardResponse.DateRangeInfo;
import com.example.cellex.dtos.response.analytics.ChartDataPoint.*;
import com.example.cellex.dtos.response.analytics.ShopAnalyticsResponse.*;
import com.example.cellex.enums.ShopStatus;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
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
 * Service xử lý Shop Analytics
 * Cung cấp các số liệu chi tiết về cửa hàng cho Admin
 * 
 * @author Cellex Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopAnalyticsService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Lấy Shop Analytics Dashboard với time filter
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return ShopAnalyticsResponse
     */
    public ShopAnalyticsResponse getShopAnalytics(LocalDate startDate, LocalDate endDate) {
        DateRangeInfo dateRange = calculateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = dateRange.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = dateRange.getEndDate().atTime(LocalTime.MAX);
        LocalDateTime prevStartDateTime = dateRange.getPreviousStartDate().atStartOfDay();
        LocalDateTime prevEndDateTime = dateRange.getPreviousEndDate().atTime(LocalTime.MAX);

        // Build Summary Cards
        List<DashboardSummaryCard> summaryCards = buildShopSummaryCards(
                startDateTime, endDateTime, prevStartDateTime, prevEndDateTime);

        // Build Overview
        ShopOverview overview = buildShopOverview(
                startDateTime, endDateTime, prevStartDateTime, prevEndDateTime);

        // Build Charts
        ShopCharts charts = buildShopCharts(dateRange.getStartDate(), dateRange.getEndDate());

        // Status Distribution
        ShopStatusDistribution statusDistribution = buildShopStatusDistribution();

        // Top Shops
        TopShops topShops = buildTopShops(startDateTime, endDateTime);

        // Pending Shops
        List<PendingShopItem> pendingShops = getPendingShops(10);

        return ShopAnalyticsResponse.builder()
                .dateRange(dateRange)
                .summaryCards(summaryCards)
                .overview(overview)
                .charts(charts)
                .statusDistribution(statusDistribution)
                .topShops(topShops)
                .pendingShops(pendingShops)
                .build();
    }

    /**
     * Build 3 Summary Cards chính cho Shop Analytics
     */
    private List<DashboardSummaryCard> buildShopSummaryCards(
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            LocalDateTime prevStartDateTime, LocalDateTime prevEndDateTime) {
        
        List<DashboardSummaryCard> cards = new ArrayList<>();

        // Card 1: Tổng cửa hàng đang hoạt động
        long activeShops = shopRepository.countByStatus(ShopStatus.APPROVED);
        long prevActiveShops = shopRepository.countByStatusAndCreatedAtBefore(ShopStatus.APPROVED, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Cửa hàng hoạt động",
                (double) activeShops,
                (double) prevActiveShops,
                "cửa hàng",
                DashboardSummaryCard.MetricType.NUMBER,
                "store",
                "Tổng số cửa hàng đang hoạt động"
        ));

        // Card 2: Cửa hàng mới (trong kỳ)
        long newShops = shopRepository.countByCreatedAtBetween(startDateTime, endDateTime);
        long prevNewShops = shopRepository.countByCreatedAtBetween(prevStartDateTime, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Cửa hàng mới",
                (double) newShops,
                (double) prevNewShops,
                "cửa hàng",
                DashboardSummaryCard.MetricType.NUMBER,
                "plus-circle",
                "Số cửa hàng mới đăng ký trong kỳ"
        ));

        // Card 3: Tổng doanh thu toàn bộ shop (trong kỳ)
        double totalRevenue = getTotalShopRevenue(startDateTime, endDateTime);
        double prevTotalRevenue = getTotalShopRevenue(prevStartDateTime, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Tổng doanh thu",
                totalRevenue,
                prevTotalRevenue,
                "VND",
                DashboardSummaryCard.MetricType.CURRENCY,
                "currency-dollar",
                "Tổng doanh thu của tất cả cửa hàng trong kỳ"
        ));

        return cards;
    }

    /**
     * Build Shop Overview
     */
    private ShopOverview buildShopOverview(
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            LocalDateTime prevStartDateTime, LocalDateTime prevEndDateTime) {
        
        long totalShops = shopRepository.count();
        long activeShops = shopRepository.countByStatus(ShopStatus.APPROVED);
        long pendingShops = shopRepository.countByStatus(ShopStatus.PENDING);
        long suspendedShops = shopRepository.countByStatus(ShopStatus.REJECTED);

        // New shops
        long newShops = shopRepository.countByCreatedAtBetween(startDateTime, endDateTime);
        long prevNewShops = shopRepository.countByCreatedAtBetween(prevStartDateTime, prevEndDateTime);
        double newShopsChange = prevNewShops > 0 ? 
                ((double)(newShops - prevNewShops) / prevNewShops) * 100 : 0;

        // Total revenue
        double totalRevenue = getTotalShopRevenue(startDateTime, endDateTime);
        double prevTotalRevenue = getTotalShopRevenue(prevStartDateTime, prevEndDateTime);
        double revenueChange = prevTotalRevenue > 0 ? 
                ((totalRevenue - prevTotalRevenue) / prevTotalRevenue) * 100 : 0;

        // Average revenue per shop
        double avgRevenuePerShop = activeShops > 0 ? totalRevenue / activeShops : 0;

        // Average orders per shop
        long totalOrders = orderRepository.countOrdersByDateRange(startDateTime, endDateTime);
        double avgOrdersPerShop = activeShops > 0 ? (double) totalOrders / activeShops : 0;

        // Average shop rating
        List<Shop> approvedShops = shopRepository.findByStatus(ShopStatus.APPROVED);
        double avgShopRating = approvedShops.stream()
                .filter(s -> s.getRating() != null && s.getRating() > 0)
                .mapToDouble(Shop::getRating)
                .average()
                .orElse(0);

        // Average products per shop
        long totalProducts = productRepository.countByIsPublishedTrue();
        double avgProductsPerShop = activeShops > 0 ? (double) totalProducts / activeShops : 0;

        return ShopOverview.builder()
                .totalShops(totalShops)
                .activeShops(activeShops)
                .newShops(newShops)
                .newShopsChange(Math.round(newShopsChange * 100.0) / 100.0)
                .pendingShops(pendingShops)
                .suspendedShops(suspendedShops)
                .totalRevenue(totalRevenue)
                .revenueChange(Math.round(revenueChange * 100.0) / 100.0)
                .averageRevenuePerShop(Math.round(avgRevenuePerShop * 100.0) / 100.0)
                .averageOrdersPerShop(Math.round(avgOrdersPerShop * 100.0) / 100.0)
                .averageShopRating(Math.round(avgShopRating * 100.0) / 100.0)
                .averageProductsPerShop(Math.round(avgProductsPerShop * 100.0) / 100.0)
                .build();
    }

    /**
     * Build Shop Charts
     */
    private ShopCharts buildShopCharts(LocalDate startDate, LocalDate endDate) {
        return ShopCharts.builder()
                .newShopsChart(buildNewShopsTimeSeriesChart(startDate, endDate))
                .revenueChart(buildShopRevenueTimeSeriesChart(startDate, endDate))
                .shopStatusChart(buildShopStatusPieChart())
                .shopRatingDistributionChart(buildShopRatingDistributionChart())
                .topShopsComparisonChart(buildTopShopsComparisonChart(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)))
                .build();
    }

    /**
     * Build New Shops Time Series Chart
     */
    private TimeSeriesChart buildNewShopsTimeSeriesChart(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        long total = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long count = shopRepository.countByCreatedAtBetween(
                    date.atStartOfDay(), date.atTime(LocalTime.MAX));
            dataPoints.add(ChartDataPoint.builder()
                    .label(date.format(formatter))
                    .value((double) count)
                    .date(date)
                    .build());
            total += count;
        }

        return TimeSeriesChart.builder()
                .title("Cửa hàng mới theo ngày")
                .chartType(ChartType.LINE)
                .data(dataPoints)
                .xAxisLabel("Ngày")
                .yAxisLabel("Số cửa hàng")
                .unit("cửa hàng")
                .total((double) total)
                .average(dataPoints.isEmpty() ? 0 : (double) total / dataPoints.size())
                .build();
    }

    /**
     * Build Shop Revenue Time Series Chart
     */
    private TimeSeriesChart buildShopRevenueTimeSeriesChart(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        double total = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            double revenue = getTotalShopRevenue(date.atStartOfDay(), date.atTime(LocalTime.MAX));
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
     * Build Shop Status Pie Chart
     */
    private PieChartData buildShopStatusPieChart() {
        List<PieSlice> slices = new ArrayList<>();
        long total = shopRepository.count();

        Map<ShopStatus, String> statusColors = Map.of(
                ShopStatus.APPROVED, "#36B37E",
                ShopStatus.PENDING, "#FFB020",
                ShopStatus.REJECTED, "#FF5630"
        );

        Map<ShopStatus, String> statusNames = Map.of(
                ShopStatus.APPROVED, "Đang hoạt động",
                ShopStatus.PENDING, "Chờ duyệt",
                ShopStatus.REJECTED, "Bị từ chối"
        );

        for (ShopStatus status : ShopStatus.values()) {
            long count = shopRepository.countByStatus(status);
            if (count > 0) {
                slices.add(PieSlice.builder()
                        .label(statusNames.getOrDefault(status, status.name()))
                        .value((double) count)
                        .percentage(total > 0 ? Math.round((double) count / total * 10000.0) / 100.0 : 0)
                        .color(statusColors.getOrDefault(status, "#8B8D97"))
                        .build());
            }
        }

        return PieChartData.builder()
                .title("Phân bổ trạng thái cửa hàng")
                .slices(slices)
                .total((double) total)
                .build();
    }

    /**
     * Build Shop Rating Distribution Chart
     */
    private BarChartData buildShopRatingDistributionChart() {
        List<Shop> allShops = shopRepository.findByStatus(ShopStatus.APPROVED);
        
        List<String> categories = Arrays.asList("5 ⭐", "4-4.9 ⭐", "3-3.9 ⭐", "2-2.9 ⭐", "1-1.9 ⭐", "Chưa đánh giá");
        List<Double> data = new ArrayList<>();
        
        data.add((double) allShops.stream().filter(s -> s.getRating() != null && s.getRating() >= 4.5).count());
        data.add((double) allShops.stream().filter(s -> s.getRating() != null && s.getRating() >= 4 && s.getRating() < 4.5).count());
        data.add((double) allShops.stream().filter(s -> s.getRating() != null && s.getRating() >= 3 && s.getRating() < 4).count());
        data.add((double) allShops.stream().filter(s -> s.getRating() != null && s.getRating() >= 2 && s.getRating() < 3).count());
        data.add((double) allShops.stream().filter(s -> s.getRating() != null && s.getRating() >= 1 && s.getRating() < 2).count());
        data.add((double) allShops.stream().filter(s -> s.getRating() == null || s.getRating() == 0).count());

        return BarChartData.builder()
                .title("Phân bổ rating cửa hàng")
                .categories(categories)
                .series(Collections.singletonList(BarSeries.builder()
                        .name("Số cửa hàng")
                        .data(data)
                        .color("#FFB020")
                        .build()))
                .build();
    }

    /**
     * Build Top Shops Comparison Chart
     */
    private BarChartData buildTopShopsComparisonChart(LocalDateTime start, LocalDateTime end) {
        List<Shop> activeShops = shopRepository.findByStatus(ShopStatus.APPROVED);
        
        List<ShopRevenueData> topShops = activeShops.stream()
                .map(shop -> {
                    List<Order> orders = orderRepository.findCompletedPaidOrdersByShopIdAndDateRange(
                            shop.getId(), start, end);
                    double revenue = orders.stream().mapToDouble(Order::getTotalAmount).sum();
                    return new ShopRevenueData(shop, revenue, (long) orders.size());
                })
                .sorted((a, b) -> Double.compare(b.revenue, a.revenue))
                .limit(5)
                .collect(Collectors.toList());

        List<String> categories = topShops.stream()
                .map(s -> s.shop.getShopName())
                .collect(Collectors.toList());
        
        List<Double> revenueData = topShops.stream()
                .map(s -> s.revenue)
                .collect(Collectors.toList());

        return BarChartData.builder()
                .title("Top 5 cửa hàng theo doanh thu")
                .categories(categories)
                .series(Collections.singletonList(BarSeries.builder()
                        .name("Doanh thu")
                        .data(revenueData)
                        .color("#3366FF")
                        .build()))
                .build();
    }

    /**
     * Build Shop Status Distribution
     */
    private ShopStatusDistribution buildShopStatusDistribution() {
        long approved = shopRepository.countByStatus(ShopStatus.APPROVED);
        long pending = shopRepository.countByStatus(ShopStatus.PENDING);
        long rejected = shopRepository.countByStatus(ShopStatus.REJECTED);
        long total = shopRepository.count();

        double approvalRate = total > 0 ? (double) approved / total * 100 : 0;

        return ShopStatusDistribution.builder()
                .approved(approved)
                .pending(pending)
                .rejected(rejected)
                .suspended(0L) // Add if you have SUSPENDED status
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                .build();
    }

    /**
     * Build Top Shops
     */
    private TopShops buildTopShops(LocalDateTime start, LocalDateTime end) {
        List<Shop> activeShops = shopRepository.findByStatus(ShopStatus.APPROVED);
        
        // Calculate metrics for each shop
        List<ShopMetrics> shopMetrics = activeShops.stream()
                .map(shop -> {
                    List<Order> orders = orderRepository.findCompletedPaidOrdersByShopIdAndDateRange(
                            shop.getId(), start, end);
                    double revenue = orders.stream().mapToDouble(Order::getTotalAmount).sum();
                    long productCount = productRepository.countByShopIdAndIsPublishedTrue(shop.getId());
                    
                    User vendor = userRepository.findById(shop.getVendorId()).orElse(null);
                    
                    return new ShopMetrics(shop, revenue, (long) orders.size(), productCount, 
                            vendor != null ? vendor.getFullName() : "Unknown");
                })
                .collect(Collectors.toList());

        // Top by Revenue
        List<TopShopItem> byRevenue = shopMetrics.stream()
                .sorted((a, b) -> Double.compare(b.revenue, a.revenue))
                .limit(10)
                .map(this::toTopShopItem)
                .collect(Collectors.toList());
        addRanks(byRevenue);

        // Top by Order Count
        List<TopShopItem> byOrderCount = shopMetrics.stream()
                .sorted((a, b) -> Long.compare(b.orderCount, a.orderCount))
                .limit(10)
                .map(this::toTopShopItem)
                .collect(Collectors.toList());
        addRanks(byOrderCount);

        // Top by Rating
        List<TopShopItem> byRating = shopMetrics.stream()
                .filter(s -> s.shop.getRating() != null && s.shop.getRating() > 0)
                .sorted((a, b) -> Double.compare(b.shop.getRating(), a.shop.getRating()))
                .limit(10)
                .map(this::toTopShopItem)
                .collect(Collectors.toList());
        addRanks(byRating);

        // Top by Product Count
        List<TopShopItem> byProductCount = shopMetrics.stream()
                .sorted((a, b) -> Long.compare(b.productCount, a.productCount))
                .limit(10)
                .map(this::toTopShopItem)
                .collect(Collectors.toList());
        addRanks(byProductCount);

        return TopShops.builder()
                .byRevenue(byRevenue)
                .byOrderCount(byOrderCount)
                .byRating(byRating)
                .byProductCount(byProductCount)
                .build();
    }

    private TopShopItem toTopShopItem(ShopMetrics metrics) {
        return TopShopItem.builder()
                .shopId(metrics.shop.getId())
                .shopName(metrics.shop.getShopName())
                .logoUrl(metrics.shop.getLogoUrl())
                .vendorName(metrics.vendorName)
                .revenue(metrics.revenue)
                .orderCount(metrics.orderCount)
                .rating(metrics.shop.getRating())
                .productCount(metrics.productCount)
                .createdAt(metrics.shop.getCreatedAt())
                .build();
    }

    private void addRanks(List<TopShopItem> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setRank(i + 1);
        }
    }

    /**
     * Get Pending Shops
     */
    private List<PendingShopItem> getPendingShops(int limit) {
        List<Shop> pendingShops = shopRepository.findByStatus(ShopStatus.PENDING);
        
        return pendingShops.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .limit(limit)
                .map(shop -> {
                    User vendor = userRepository.findById(shop.getVendorId()).orElse(null);
                    long daysPending = shop.getCreatedAt() != null ? 
                            ChronoUnit.DAYS.between(shop.getCreatedAt(), LocalDateTime.now()) : 0;
                    
                    return PendingShopItem.builder()
                            .shopId(shop.getId())
                            .shopName(shop.getShopName())
                            .logoUrl(shop.getLogoUrl())
                            .vendorName(vendor != null ? vendor.getFullName() : "Unknown")
                            .vendorEmail(vendor != null ? vendor.getEmail() : null)
                            .status(shop.getStatus().name())
                            .createdAt(shop.getCreatedAt())
                            .daysPending(daysPending)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================

    private DateRangeInfo calculateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(periodDays - 1);

        return DateRangeInfo.builder()
                .startDate(startDate)
                .endDate(endDate)
                .period("CUSTOM")
                .previousStartDate(prevStartDate)
                .previousEndDate(prevEndDate)
                .build();
    }

    private double getTotalShopRevenue(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        return orders.stream().mapToDouble(Order::getTotalAmount).sum();
    }

    // Helper classes
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

    private static class ShopMetrics {
        Shop shop;
        Double revenue;
        Long orderCount;
        Long productCount;
        String vendorName;

        ShopMetrics(Shop shop, Double revenue, Long orderCount, Long productCount, String vendorName) {
            this.shop = shop;
            this.revenue = revenue;
            this.orderCount = orderCount;
            this.productCount = productCount;
            this.vendorName = vendorName;
        }
    }
}
