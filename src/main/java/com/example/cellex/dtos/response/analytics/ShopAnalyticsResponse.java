package com.example.cellex.dtos.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO Response cho trang Shop Analytics (Admin)
 * Hiển thị chi tiết các số liệu về cửa hàng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopAnalyticsResponse {

    /**
     * Khoảng thời gian
     */
    private AdminMainDashboardResponse.DateRangeInfo dateRange;

    /**
     * 3 Summary Cards chính
     * - Tổng cửa hàng đang hoạt động
     * - Cửa hàng mới (trong kỳ)
     * - Tổng doanh thu toàn bộ shop (trong kỳ)
     */
    private List<DashboardSummaryCard> summaryCards;

    /**
     * Shop Overview
     */
    private ShopOverview overview;

    /**
     * Charts
     */
    private ShopCharts charts;

    /**
     * Shop Status Distribution
     */
    private ShopStatusDistribution statusDistribution;

    /**
     * Top Shops
     */
    private TopShops topShops;

    /**
     * Shops mới gần đây / chờ duyệt
     */
    private List<PendingShopItem> pendingShops;

    // ==================== NESTED CLASSES ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShopOverview {
        /**
         * Tổng số shop
         */
        private Long totalShops;

        /**
         * Shop đang hoạt động
         */
        private Long activeShops;

        /**
         * Shop mới trong kỳ
         */
        private Long newShops;
        private Double newShopsChange;

        /**
         * Shop chờ duyệt
         */
        private Long pendingShops;

        /**
         * Shop bị từ chối/tạm khóa
         */
        private Long suspendedShops;

        /**
         * Tổng doanh thu của tất cả shop trong kỳ
         */
        private Double totalRevenue;
        private Double revenueChange;

        /**
         * Doanh thu trung bình mỗi shop
         */
        private Double averageRevenuePerShop;

        /**
         * Số đơn trung bình mỗi shop
         */
        private Double averageOrdersPerShop;

        /**
         * Rating trung bình của tất cả shop
         */
        private Double averageShopRating;

        /**
         * Số sản phẩm trung bình mỗi shop
         */
        private Double averageProductsPerShop;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShopCharts {
        /**
         * Biểu đồ shop mới theo thời gian
         */
        private ChartDataPoint.TimeSeriesChart newShopsChart;

        /**
         * Biểu đồ tổng doanh thu theo thời gian
         */
        private ChartDataPoint.TimeSeriesChart revenueChart;

        /**
         * Biểu đồ phân bổ shop theo trạng thái
         */
        private ChartDataPoint.PieChartData shopStatusChart;

        /**
         * Biểu đồ phân bổ shop theo rating
         */
        private ChartDataPoint.BarChartData shopRatingDistributionChart;

        /**
         * Biểu đồ so sánh top shops
         */
        private ChartDataPoint.BarChartData topShopsComparisonChart;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShopStatusDistribution {
        private Long approved;
        private Long pending;
        private Long rejected;
        private Long suspended;
        private Double approvalRate; // Tỷ lệ được duyệt
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopShops {
        /**
         * Top shop doanh thu cao nhất
         */
        private List<TopShopItem> byRevenue;

        /**
         * Top shop nhiều đơn hàng nhất
         */
        private List<TopShopItem> byOrderCount;

        /**
         * Top shop rating cao nhất
         */
        private List<TopShopItem> byRating;

        /**
         * Top shop nhiều sản phẩm nhất
         */
        private List<TopShopItem> byProductCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopShopItem {
        private Integer rank;
        private String shopId;
        private String shopName;
        private String logoUrl;
        private String vendorName;
        private Double revenue;
        private Long orderCount;
        private Double rating;
        private Long productCount;
        private Long reviewCount;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PendingShopItem {
        private String shopId;
        private String shopName;
        private String logoUrl;
        private String vendorName;
        private String vendorEmail;
        private String status;
        private LocalDateTime createdAt;
        private Long daysPending; // Số ngày chờ duyệt
    }
}
