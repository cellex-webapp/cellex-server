package com.example.cellex.dtos.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO Response cho trang Product Analytics (Admin)
 * Hiển thị chi tiết các số liệu về sản phẩm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductAnalyticsResponse {

    /**
     * Khoảng thời gian
     */
    private AdminMainDashboardResponse.DateRangeInfo dateRange;

    /**
     * 3 Summary Cards chính
     * - Tổng sản phẩm đang bán
     * - Sản phẩm mới (trong kỳ)
     * - Tổng số lượng đã bán (trong kỳ)
     */
    private List<DashboardSummaryCard> summaryCards;

    /**
     * Product Overview
     */
    private ProductOverview overview;

    /**
     * Charts
     */
    private ProductCharts charts;

    /**
     * Category Performance
     */
    private List<CategoryPerformance> categoryPerformance;

    /**
     * Top Products (nhiều loại)
     */
    private TopProducts topProducts;

    /**
     * Sản phẩm mới gần đây
     */
    private List<RecentProductItem> recentProducts;

    // ==================== NESTED CLASSES ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductOverview {
        /**
         * Tổng số sản phẩm đang bán
         */
        private Long totalActiveProducts;
        
        /**
         * Sản phẩm mới trong kỳ
         */
        private Long newProducts;
        private Double newProductsChange;

        /**
         * Tổng số lượng đã bán trong kỳ
         */
        private Long totalQuantitySold;
        private Double quantitySoldChange;

        /**
         * Doanh thu từ sản phẩm trong kỳ
         */
        private Double totalProductRevenue;
        private Double revenueChange;

        /**
         * Giá bán trung bình
         */
        private Double averagePrice;

        /**
         * Sản phẩm hết hàng (stock = 0)
         */
        private Long outOfStockProducts;

        /**
         * Sản phẩm sắp hết hàng (stock < 10)
         */
        private Long lowStockProducts;

        /**
         * Đánh giá trung bình của tất cả sản phẩm
         */
        private Double averageRating;

        /**
         * Tổng số review
         */
        private Long totalReviews;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductCharts {
        /**
         * Biểu đồ số lượng bán theo thời gian
         */
        private ChartDataPoint.TimeSeriesChart salesQuantityChart;

        /**
         * Biểu đồ doanh thu sản phẩm theo thời gian
         */
        private ChartDataPoint.TimeSeriesChart productRevenueChart;

        /**
         * Biểu đồ phân bổ doanh thu theo danh mục
         */
        private ChartDataPoint.PieChartData revenueByCategoryChart;

        /**
         * Biểu đồ số lượng sản phẩm theo danh mục
         */
        private ChartDataPoint.PieChartData productsByCategoryChart;

        /**
         * Biểu đồ phân bổ rating sản phẩm
         */
        private ChartDataPoint.BarChartData ratingDistributionChart;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryPerformance {
        private String categoryId;
        private String categoryName;
        private Long productCount;
        private Long quantitySold;
        private Double revenue;
        private Double averageRating;
        private Double revenueShare; // % đóng góp vào tổng doanh thu
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProducts {
        /**
         * Top sản phẩm bán chạy nhất
         */
        private List<TopProductItem> byQuantitySold;

        /**
         * Top sản phẩm doanh thu cao nhất
         */
        private List<TopProductItem> byRevenue;

        /**
         * Top sản phẩm được đánh giá cao nhất
         */
        private List<TopProductItem> byRating;

        /**
         * Top sản phẩm được xem nhiều nhất (nếu có tracking)
         */
        private List<TopProductItem> byViews;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProductItem {
        private Integer rank;
        private String productId;
        private String productName;
        private String imageUrl;
        private String shopId;
        private String shopName;
        private String categoryName;
        private Double price;
        private Long quantitySold;
        private Double revenue;
        private Double rating;
        private Long reviewCount;
        private Long viewCount;
        private Integer stock;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentProductItem {
        private String productId;
        private String productName;
        private String imageUrl;
        private String shopName;
        private String categoryName;
        private Double price;
        private Integer stock;
        private LocalDateTime createdAt;
    }
}
