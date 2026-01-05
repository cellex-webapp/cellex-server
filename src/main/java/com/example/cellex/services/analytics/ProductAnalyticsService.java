package com.example.cellex.services.analytics;

import com.example.cellex.dtos.response.analytics.*;
import com.example.cellex.dtos.response.analytics.AdminMainDashboardResponse.DateRangeInfo;
import com.example.cellex.dtos.response.analytics.ChartDataPoint.*;
import com.example.cellex.dtos.response.analytics.ProductAnalyticsResponse.*;
import com.example.cellex.models.category.Category;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.repositories.category.CategoryRepository;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.review.ReviewRepository;
import com.example.cellex.repositories.shop.ShopRepository;
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
 * Service xử lý Product Analytics
 * Cung cấp các số liệu chi tiết về sản phẩm cho Admin
 * 
 * @author Cellex Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductAnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Lấy Product Analytics Dashboard với time filter
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return ProductAnalyticsResponse
     */
    public ProductAnalyticsResponse getProductAnalytics(LocalDate startDate, LocalDate endDate) {
        DateRangeInfo dateRange = calculateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = dateRange.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = dateRange.getEndDate().atTime(LocalTime.MAX);
        LocalDateTime prevStartDateTime = dateRange.getPreviousStartDate().atStartOfDay();
        LocalDateTime prevEndDateTime = dateRange.getPreviousEndDate().atTime(LocalTime.MAX);

        // Build Summary Cards
        List<DashboardSummaryCard> summaryCards = buildProductSummaryCards(
                startDateTime, endDateTime, prevStartDateTime, prevEndDateTime);

        // Build Overview
        ProductOverview overview = buildProductOverview(
                startDateTime, endDateTime, prevStartDateTime, prevEndDateTime);

        // Build Charts
        ProductCharts charts = buildProductCharts(dateRange.getStartDate(), dateRange.getEndDate());

        // Category Performance
        List<CategoryPerformance> categoryPerformance = buildCategoryPerformance(startDateTime, endDateTime);

        // Top Products
        TopProducts topProducts = buildTopProducts(startDateTime, endDateTime);

        // Recent Products
        List<RecentProductItem> recentProducts = getRecentProducts(10);

        return ProductAnalyticsResponse.builder()
                .dateRange(dateRange)
                .summaryCards(summaryCards)
                .overview(overview)
                .charts(charts)
                .categoryPerformance(categoryPerformance)
                .topProducts(topProducts)
                .recentProducts(recentProducts)
                .build();
    }

    /**
     * Build 3 Summary Cards chính cho Product Analytics
     */
    private List<DashboardSummaryCard> buildProductSummaryCards(
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            LocalDateTime prevStartDateTime, LocalDateTime prevEndDateTime) {
        
        List<DashboardSummaryCard> cards = new ArrayList<>();

        // Card 1: Tổng sản phẩm đang bán
        long totalProducts = productRepository.countByIsPublishedTrue();
        long prevTotalProducts = productRepository.countByIsPublishedTrueAndCreatedAtBefore(prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Sản phẩm đang bán",
                (double) totalProducts,
                (double) prevTotalProducts,
                "sản phẩm",
                DashboardSummaryCard.MetricType.NUMBER,
                "package",
                "Tổng số sản phẩm đang được bán"
        ));

        // Card 2: Sản phẩm mới (trong kỳ)
        long newProducts = productRepository.countByIsPublishedTrueAndCreatedAtBetween(startDateTime, endDateTime);
        long prevNewProducts = productRepository.countByIsPublishedTrueAndCreatedAtBetween(prevStartDateTime, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Sản phẩm mới",
                (double) newProducts,
                (double) prevNewProducts,
                "sản phẩm",
                DashboardSummaryCard.MetricType.NUMBER,
                "plus-circle",
                "Số sản phẩm mới đăng bán trong kỳ"
        ));

        // Card 3: Số lượng đã bán (trong kỳ)
        long quantitySold = getTotalQuantitySold(startDateTime, endDateTime);
        long prevQuantitySold = getTotalQuantitySold(prevStartDateTime, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Số lượng đã bán",
                (double) quantitySold,
                (double) prevQuantitySold,
                "sản phẩm",
                DashboardSummaryCard.MetricType.NUMBER,
                "shopping-bag",
                "Tổng số lượng sản phẩm đã bán trong kỳ"
        ));

        return cards;
    }

    /**
     * Build Product Overview
     */
    private ProductOverview buildProductOverview(
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            LocalDateTime prevStartDateTime, LocalDateTime prevEndDateTime) {
        
        long totalActiveProducts = productRepository.countByIsPublishedTrue();
        
        // New products
        long newProducts = productRepository.countByIsPublishedTrueAndCreatedAtBetween(startDateTime, endDateTime);
        long prevNewProducts = productRepository.countByIsPublishedTrueAndCreatedAtBetween(prevStartDateTime, prevEndDateTime);
        double newProductsChange = prevNewProducts > 0 ? 
                ((double)(newProducts - prevNewProducts) / prevNewProducts) * 100 : 0;

        // Quantity sold
        long quantitySold = getTotalQuantitySold(startDateTime, endDateTime);
        long prevQuantitySold = getTotalQuantitySold(prevStartDateTime, prevEndDateTime);
        double quantitySoldChange = prevQuantitySold > 0 ? 
                ((double)(quantitySold - prevQuantitySold) / prevQuantitySold) * 100 : 0;

        // Revenue
        double productRevenue = getProductRevenue(startDateTime, endDateTime);
        double prevProductRevenue = getProductRevenue(prevStartDateTime, prevEndDateTime);
        double revenueChange = prevProductRevenue > 0 ? 
                ((productRevenue - prevProductRevenue) / prevProductRevenue) * 100 : 0;

        // Average price
        List<Product> allProducts = productRepository.findAllByIsPublishedTrue();
        double averagePrice = allProducts.stream()
                .mapToDouble(p -> p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice())
                .average()
                .orElse(0);

        // Stock status
        long outOfStockProducts = allProducts.stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= 0)
                .count();
        long lowStockProducts = allProducts.stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0 && p.getStockQuantity() < 10)
                .count();

        // Ratings
        double averageRating = allProducts.stream()
                .filter(p -> p.getAverageRating() != null && p.getAverageRating() > 0)
                .mapToDouble(Product::getAverageRating)
                .average()
                .orElse(0);
        long totalReviews = allProducts.stream()
                .mapToLong(p -> p.getReviewCount() != null ? p.getReviewCount() : 0)
                .sum();

        return ProductOverview.builder()
                .totalActiveProducts(totalActiveProducts)
                .newProducts(newProducts)
                .newProductsChange(Math.round(newProductsChange * 100.0) / 100.0)
                .totalQuantitySold(quantitySold)
                .quantitySoldChange(Math.round(quantitySoldChange * 100.0) / 100.0)
                .totalProductRevenue(productRevenue)
                .revenueChange(Math.round(revenueChange * 100.0) / 100.0)
                .averagePrice(Math.round(averagePrice * 100.0) / 100.0)
                .outOfStockProducts(outOfStockProducts)
                .lowStockProducts(lowStockProducts)
                .averageRating(Math.round(averageRating * 100.0) / 100.0)
                .totalReviews(totalReviews)
                .build();
    }

    /**
     * Build Product Charts
     */
    private ProductCharts buildProductCharts(LocalDate startDate, LocalDate endDate) {
        return ProductCharts.builder()
                .salesQuantityChart(buildSalesQuantityTimeSeriesChart(startDate, endDate))
                .productRevenueChart(buildProductRevenueTimeSeriesChart(startDate, endDate))
                .revenueByCategoryChart(buildRevenueByCategoryPieChart(startDate, endDate))
                .productsByCategoryChart(buildProductsByCategoryPieChart())
                .ratingDistributionChart(buildRatingDistributionBarChart())
                .build();
    }

    /**
     * Build Sales Quantity Time Series Chart
     */
    private TimeSeriesChart buildSalesQuantityTimeSeriesChart(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        long total = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long quantity = getTotalQuantitySold(date.atStartOfDay(), date.atTime(LocalTime.MAX));
            dataPoints.add(ChartDataPoint.builder()
                    .label(date.format(formatter))
                    .value((double) quantity)
                    .date(date)
                    .build());
            total += quantity;
        }

        return TimeSeriesChart.builder()
                .title("Số lượng bán theo ngày")
                .chartType(ChartType.BAR)
                .data(dataPoints)
                .xAxisLabel("Ngày")
                .yAxisLabel("Số lượng")
                .unit("sản phẩm")
                .total((double) total)
                .average(dataPoints.isEmpty() ? 0 : (double) total / dataPoints.size())
                .build();
    }

    /**
     * Build Product Revenue Time Series Chart
     */
    private TimeSeriesChart buildProductRevenueTimeSeriesChart(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        double total = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            double revenue = getProductRevenue(date.atStartOfDay(), date.atTime(LocalTime.MAX));
            dataPoints.add(ChartDataPoint.builder()
                    .label(date.format(formatter))
                    .value(revenue)
                    .date(date)
                    .build());
            total += revenue;
        }

        return TimeSeriesChart.builder()
                .title("Doanh thu sản phẩm theo ngày")
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
     * Build Revenue by Category Pie Chart
     */
    private PieChartData buildRevenueByCategoryPieChart(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByDateRange(startDateTime, endDateTime);
        Map<String, Double> revenueByCategory = new HashMap<>();

        for (Order order : completedOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    // Skip items with null productId
                    if (item.getProductId() == null) {
                        log.warn("OrderItem with null productId found in order: {}", order.getId());
                        continue;
                    }
                    String categoryId = productRepository.findById(item.getProductId())
                            .map(Product::getCategoryId)
                            .orElse(null);
                    if (categoryId != null) {
                        revenueByCategory.merge(categoryId, item.getSubtotal(), Double::sum);
                    }
                }
            }
        }

        List<PieSlice> slices = new ArrayList<>();
        double total = revenueByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
        String[] colors = {"#3366FF", "#00B8D9", "#36B37E", "#FFB020", "#FF5630", "#8B8D97"};
        int colorIndex = 0;

        List<Map.Entry<String, Double>> sortedEntries = revenueByCategory.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(sortedEntries.size(), 5); i++) {
            Map.Entry<String, Double> entry = sortedEntries.get(i);
            String categoryName = getCategoryName(entry.getKey());
            slices.add(PieSlice.builder()
                    .label(categoryName)
                    .value(entry.getValue())
                    .percentage(total > 0 ? Math.round(entry.getValue() / total * 10000.0) / 100.0 : 0)
                    .color(colors[colorIndex % colors.length])
                    .build());
            colorIndex++;
        }

        if (sortedEntries.size() > 5) {
            double othersValue = sortedEntries.subList(5, sortedEntries.size()).stream()
                    .mapToDouble(Map.Entry::getValue).sum();
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
     * Build Products by Category Pie Chart
     */
    private PieChartData buildProductsByCategoryPieChart() {
        List<Product> allProducts = productRepository.findAllByIsPublishedTrue();
        Map<String, Long> productsByCategory = allProducts.stream()
                .filter(p -> p.getCategoryId() != null)
                .collect(Collectors.groupingBy(Product::getCategoryId, Collectors.counting()));

        List<PieSlice> slices = new ArrayList<>();
        long total = allProducts.size();
        String[] colors = {"#3366FF", "#00B8D9", "#36B37E", "#FFB020", "#FF5630", "#8B8D97"};
        int colorIndex = 0;

        List<Map.Entry<String, Long>> sortedEntries = productsByCategory.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(sortedEntries.size(), 5); i++) {
            Map.Entry<String, Long> entry = sortedEntries.get(i);
            String categoryName = getCategoryName(entry.getKey());
            slices.add(PieSlice.builder()
                    .label(categoryName)
                    .value((double) entry.getValue())
                    .percentage(total > 0 ? Math.round((double) entry.getValue() / total * 10000.0) / 100.0 : 0)
                    .color(colors[colorIndex % colors.length])
                    .build());
            colorIndex++;
        }

        if (sortedEntries.size() > 5) {
            long othersValue = sortedEntries.subList(5, sortedEntries.size()).stream()
                    .mapToLong(Map.Entry::getValue).sum();
            slices.add(PieSlice.builder()
                    .label("Khác")
                    .value((double) othersValue)
                    .percentage(total > 0 ? Math.round((double) othersValue / total * 10000.0) / 100.0 : 0)
                    .color("#8B8D97")
                    .build());
        }

        return PieChartData.builder()
                .title("Số lượng sản phẩm theo danh mục")
                .slices(slices)
                .total((double) total)
                .build();
    }

    /**
     * Build Rating Distribution Bar Chart
     */
    private BarChartData buildRatingDistributionBarChart() {
        List<Product> allProducts = productRepository.findAllByIsPublishedTrue();
        
        List<String> categories = Arrays.asList("5 ⭐", "4 ⭐", "3 ⭐", "2 ⭐", "1 ⭐");
        List<Double> data = new ArrayList<>();
        
        for (int rating = 5; rating >= 1; rating--) {
            int r = rating;
            long count = allProducts.stream()
                    .filter(p -> p.getAverageRating() != null && 
                            p.getAverageRating() >= r - 0.5 && p.getAverageRating() < r + 0.5)
                    .count();
            data.add((double) count);
        }

        return BarChartData.builder()
                .title("Phân bổ rating sản phẩm")
                .categories(categories)
                .series(Collections.singletonList(BarSeries.builder()
                        .name("Số sản phẩm")
                        .data(data)
                        .color("#FFB020")
                        .build()))
                .build();
    }

    /**
     * Build Category Performance
     */
    private List<CategoryPerformance> buildCategoryPerformance(LocalDateTime start, LocalDateTime end) {
        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        Map<String, Long> productCountByCategory = new HashMap<>();
        Map<String, Long> quantityByCategory = new HashMap<>();
        Map<String, Double> revenueByCategory = new HashMap<>();

        for (Order order : completedOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    // Skip items with null productId
                    if (item.getProductId() == null) {
                        log.warn("OrderItem with null productId found in order: {}", order.getId());
                        continue;
                    }
                    String categoryId = productRepository.findById(item.getProductId())
                            .map(Product::getCategoryId)
                            .orElse(null);
                    if (categoryId != null) {
                        productCountByCategory.merge(categoryId, 1L, Long::sum);
                        quantityByCategory.merge(categoryId, (long) item.getQuantity(), Long::sum);
                        revenueByCategory.merge(categoryId, item.getSubtotal(), Double::sum);
                    }
                }
            }
        }

        double totalRevenue = revenueByCategory.values().stream().mapToDouble(Double::doubleValue).sum();

        return revenueByCategory.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(entry -> {
                    String categoryId = entry.getKey();
                    Category category = categoryRepository.findById(categoryId).orElse(null);
                    
                    // Get average rating for products in this category
                    List<Product> categoryProducts = productRepository.findByCategoryIdAndIsPublishedTrue(
                            categoryId, PageRequest.of(0, 100)).getContent();
                    double avgRating = categoryProducts.stream()
                            .filter(p -> p.getAverageRating() != null && p.getAverageRating() > 0)
                            .mapToDouble(Product::getAverageRating)
                            .average()
                            .orElse(0);

                    return CategoryPerformance.builder()
                            .categoryId(categoryId)
                            .categoryName(category != null ? category.getName() : "Không phân loại")
                            .productCount(productCountByCategory.getOrDefault(categoryId, 0L))
                            .quantitySold(quantityByCategory.getOrDefault(categoryId, 0L))
                            .revenue(entry.getValue())
                            .averageRating(Math.round(avgRating * 100.0) / 100.0)
                            .revenueShare(totalRevenue > 0 ? 
                                    Math.round(entry.getValue() / totalRevenue * 10000.0) / 100.0 : 0)
                            .build();
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Build Top Products
     */
    private TopProducts buildTopProducts(LocalDateTime start, LocalDateTime end) {
        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        
        // Aggregate product sales
        Map<String, ProductSalesData> salesByProduct = new HashMap<>();
        for (Order order : completedOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    String productId = item.getProductId();
                    String categoryId = productRepository.findById(item.getProductId())
                            .map(Product::getCategoryId)
                            .orElse(null);
                    ProductSalesData data = salesByProduct.computeIfAbsent(productId, 
                            k -> new ProductSalesData(productId, item.getProductName(), 
                                    item.getProductImage(), order.getShopId(), order.getShopName(),
                                    categoryId, 0L, 0.0));
                    data.totalQuantity += item.getQuantity();
                    data.totalRevenue += item.getSubtotal();
                }
            }
        }

        // Top by Quantity
        List<TopProductItem> byQuantity = salesByProduct.values().stream()
                .sorted((a, b) -> Long.compare(b.totalQuantity, a.totalQuantity))
                .limit(10)
                .map(this::toTopProductItem)
                .collect(Collectors.toList());
        addRanks(byQuantity);

        // Top by Revenue
        List<TopProductItem> byRevenue = salesByProduct.values().stream()
                .sorted((a, b) -> Double.compare(b.totalRevenue, a.totalRevenue))
                .limit(10)
                .map(this::toTopProductItem)
                .collect(Collectors.toList());
        addRanks(byRevenue);

        // Top by Rating
        List<Product> topRatedProducts = productRepository.findAllByIsPublishedTrue().stream()
                .filter(p -> p.getAverageRating() != null && p.getAverageRating() > 0)
                .sorted((a, b) -> Double.compare(b.getAverageRating(), a.getAverageRating()))
                .limit(10)
                .collect(Collectors.toList());
        
        List<TopProductItem> byRating = topRatedProducts.stream()
                .map(p -> {
                    Shop shop = shopRepository.findById(p.getShopId()).orElse(null);
                    return TopProductItem.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .imageUrl(p.getImages() != null && !p.getImages().isEmpty() ? p.getImages().get(0) : null)
                            .shopId(p.getShopId())
                            .shopName(shop != null ? shop.getShopName() : "Unknown")
                            .categoryName(getCategoryName(p.getCategoryId()))
                            .price(p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice())
                            .rating(p.getAverageRating())
                            .reviewCount((long) (p.getReviewCount() != null ? p.getReviewCount() : 0))
                            .stock(p.getStockQuantity())
                            .build();
                })
                .collect(Collectors.toList());
        addRanks(byRating);

        return TopProducts.builder()
                .byQuantitySold(byQuantity)
                .byRevenue(byRevenue)
                .byRating(byRating)
                .byViews(Collections.emptyList()) // Not implemented yet
                .build();
    }

    private TopProductItem toTopProductItem(ProductSalesData data) {
        Product product = productRepository.findById(data.productId).orElse(null);
        return TopProductItem.builder()
                .productId(data.productId)
                .productName(data.productName)
                .imageUrl(data.productImage)
                .shopId(data.shopId)
                .shopName(data.shopName)
                .categoryName(getCategoryName(data.categoryId))
                .price(product != null ? (product.getFinalPrice() != null ? product.getFinalPrice() : product.getPrice()) : 0)
                .quantitySold(data.totalQuantity)
                .revenue(data.totalRevenue)
                .rating(product != null ? product.getAverageRating() : null)
                .reviewCount(product != null ? (long)(product.getReviewCount() != null ? product.getReviewCount() : 0) : 0)
                .stock(product != null ? product.getStockQuantity() : null)
                .build();
    }

    private void addRanks(List<TopProductItem> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setRank(i + 1);
        }
    }

    /**
     * Get Recent Products
     */
    private List<RecentProductItem> getRecentProducts(int limit) {
        List<Product> products = productRepository.findAllByIsPublishedTrue(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        
        return products.stream()
                .map(p -> {
                    Shop shop = shopRepository.findById(p.getShopId()).orElse(null);
                    return RecentProductItem.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .imageUrl(p.getImages() != null && !p.getImages().isEmpty() ? p.getImages().get(0) : null)
                            .shopName(shop != null ? shop.getShopName() : "Unknown")
                            .categoryName(getCategoryName(p.getCategoryId()))
                            .price(p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice())
                            .stock(p.getStockQuantity())
                            .createdAt(p.getCreatedAt())
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

    private long getTotalQuantitySold(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        return orders.stream()
                .filter(o -> o.getItems() != null)
                .flatMap(o -> o.getItems().stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();
    }

    private double getProductRevenue(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        return orders.stream()
                .filter(o -> o.getItems() != null)
                .flatMap(o -> o.getItems().stream())
                .mapToDouble(OrderItem::getSubtotal)
                .sum();
    }

    private String getCategoryName(String categoryId) {
        if (categoryId == null) return "Không phân loại";
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse("Không phân loại");
    }

    // Helper class
    private static class ProductSalesData {
        String productId;
        String productName;
        String productImage;
        String shopId;
        String shopName;
        String categoryId;
        Long totalQuantity;
        Double totalRevenue;

        ProductSalesData(String productId, String productName, String productImage, 
                        String shopId, String shopName, String categoryId,
                        Long totalQuantity, Double totalRevenue) {
            this.productId = productId;
            this.productName = productName;
            this.productImage = productImage;
            this.shopId = shopId;
            this.shopName = shopName;
            this.categoryId = categoryId;
            this.totalQuantity = totalQuantity;
            this.totalRevenue = totalRevenue;
        }
    }
}
