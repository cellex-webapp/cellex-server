package com.example.cellex.dtos.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO Response cho Admin Dashboard chính
 * Hiển thị tổng quan hệ thống với 3 summary cards + charts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminMainDashboardResponse {

    /**
     * Khoảng thời gian được chọn
     */
    private DateRangeInfo dateRange;

    /**
     * 3 Summary Cards chính hiển thị ở đầu dashboard
     * - Tổng doanh thu
     * - Tổng đơn hàng
     * - Tổng khách hàng mới
     */
    private List<DashboardSummaryCard> summaryCards;

    /**
     * KPIs phụ (hiển thị nhỏ hơn bên dưới summary cards)
     */
    private SecondaryKPIs secondaryKPIs;

    /**
     * Charts Section
     */
    private DashboardCharts charts;

    /**
     * Top performers
     */
    private TopPerformers topPerformers;

    /**
     * Hoạt động gần đây
     */
    private RecentActivities recentActivities;

    // ==================== NESTED CLASSES ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DateRangeInfo {
        private LocalDate startDate;
        private LocalDate endDate;
        private String period; // "TODAY", "THIS_WEEK", "THIS_MONTH", "THIS_YEAR", "CUSTOM"
        private LocalDate previousStartDate;
        private LocalDate previousEndDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SecondaryKPIs {
        /**
         * Giá trị đơn hàng trung bình
         */
        private Double averageOrderValue;
        private Double aovChange; // % thay đổi

        /**
         * Tỷ lệ chuyển đổi (đơn hoàn thành / tổng đơn)
         */
        private Double conversionRate;
        private Double conversionRateChange;

        /**
         * Tỷ lệ hủy đơn
         */
        private Double cancellationRate;
        private Double cancellationRateChange;

        /**
         * Số shop đang hoạt động
         */
        private Long activeShops;
        private Long newShopsThisPeriod;

        /**
         * Số sản phẩm đang bán
         */
        private Long activeProducts;
        private Long newProductsThisPeriod;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardCharts {
        /**
         * Biểu đồ doanh thu theo thời gian (Line/Area chart)
         */
        private ChartDataPoint.TimeSeriesChart revenueChart;

        /**
         * Biểu đồ số đơn hàng theo thời gian
         */
        private ChartDataPoint.TimeSeriesChart ordersChart;

        /**
         * Biểu đồ phân bổ trạng thái đơn hàng (Pie chart)
         */
        private ChartDataPoint.PieChartData orderStatusDistribution;

        /**
         * Biểu đồ doanh thu theo danh mục sản phẩm
         */
        private ChartDataPoint.PieChartData revenueByCategoryChart;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopPerformers {
        /**
         * Top 5 shop có doanh thu cao nhất
         */
        private List<TopShopItem> topShops;

        /**
         * Top 5 sản phẩm bán chạy nhất
         */
        private List<TopProductItem> topProducts;

        /**
         * Top 5 khách hàng chi tiêu nhiều nhất
         */
        private List<TopCustomerItem> topCustomers;
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
        private Double revenue;
        private Long orderCount;
        private Double rating;
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
        private String shopName;
        private Long quantitySold;
        private Double revenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopCustomerItem {
        private Integer rank;
        private String userId;
        private String fullName;
        private String avatarUrl;
        private Double totalSpent;
        private Long orderCount;
        private LocalDateTime lastOrderDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentActivities {
        /**
         * Đơn hàng gần đây
         */
        private List<RecentOrderItem> recentOrders;

        /**
         * Shop mới đăng ký
         */
        private List<RecentShopItem> newShops;

        /**
         * Khách hàng mới
         */
        private List<RecentUserItem> newUsers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentOrderItem {
        private String orderId;
        private String customerName;
        private String shopName;
        private Double totalAmount;
        private String status;
        private String paymentMethod;
        private Boolean isPaid;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentShopItem {
        private String shopId;
        private String shopName;
        private String logoUrl;
        private String vendorName;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentUserItem {
        private String userId;
        private String fullName;
        private String email;
        private String avatarUrl;
        private LocalDateTime createdAt;
    }
}
