package com.example.cellex.dtos.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO Response cho trang Customer Analytics (Admin)
 * Hiển thị chi tiết các số liệu về khách hàng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerAnalyticsResponse {

    /**
     * Khoảng thời gian
     */
    private AdminMainDashboardResponse.DateRangeInfo dateRange;

    /**
     * 3 Summary Cards chính
     * - Tổng khách hàng
     * - Khách hàng mới (trong kỳ)
     * - Tỷ lệ khách hàng quay lại
     */
    private List<DashboardSummaryCard> summaryCards;

    /**
     * Customer Overview
     */
    private CustomerOverview overview;

    /**
     * Charts
     */
    private CustomerCharts charts;

    /**
     * Customer Segments
     */
    private CustomerSegments segments;

    /**
     * Top Customers
     */
    private List<AdminMainDashboardResponse.TopCustomerItem> topCustomers;

    /**
     * Khách hàng mới gần đây
     */
    private List<AdminMainDashboardResponse.RecentUserItem> recentCustomers;

    // ==================== NESTED CLASSES ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerOverview {
        /**
         * Tổng số khách hàng
         */
        private Long totalCustomers;
        
        /**
         * Khách hàng mới trong kỳ
         */
        private Long newCustomers;
        private Double newCustomersChange;

        /**
         * Khách hàng hoạt động (có đơn hàng trong kỳ)
         */
        private Long activeCustomers;
        private Double activeCustomersChange;

        /**
         * Tỷ lệ khách hàng quay lại (có > 1 đơn hàng)
         */
        private Double returnRate;
        private Double returnRateChange;

        /**
         * Giá trị trung bình mỗi khách hàng (Customer Lifetime Value - đơn giản)
         */
        private Double averageCustomerValue;
        private Double acvChange;

        /**
         * Số đơn trung bình mỗi khách hàng
         */
        private Double averageOrdersPerCustomer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerCharts {
        /**
         * Biểu đồ khách hàng mới theo thời gian
         */
        private ChartDataPoint.TimeSeriesChart newCustomersChart;

        /**
         * Biểu đồ khách hàng hoạt động theo thời gian
         */
        private ChartDataPoint.TimeSeriesChart activeCustomersChart;

        /**
         * Biểu đồ phân bổ khách hàng theo segment
         */
        private ChartDataPoint.PieChartData customerSegmentChart;

        /**
         * Biểu đồ phân bổ khách hàng theo số đơn hàng
         */
        private ChartDataPoint.PieChartData customersByOrderCountChart;

        /**
         * Biểu đồ chi tiêu của khách hàng theo thời gian
         */
        private ChartDataPoint.TimeSeriesChart customerSpendingChart;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerSegments {
        /**
         * Khách hàng VIP (chi tiêu cao)
         */
        private SegmentInfo vipCustomers;

        /**
         * Khách hàng thường xuyên
         */
        private SegmentInfo regularCustomers;

        /**
         * Khách hàng mới
         */
        private SegmentInfo newCustomers;

        /**
         * Khách hàng không hoạt động (không có đơn > 30 ngày)
         */
        private SegmentInfo inactiveCustomers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SegmentInfo {
        private String segmentName;
        private Long count;
        private Double percentage;
        private Double averageSpending;
        private Double averageOrders;
    }
}
