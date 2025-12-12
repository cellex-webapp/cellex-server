package com.example.cellex.dtos.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO Response cho Vendor Dashboard
 * Chứa các thông tin thống kê của một cửa hàng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorDashboardResponse {

    /**
     * Thông tin cửa hàng
     */
    private ShopInfo shopInfo;

    /**
     * Thống kê doanh thu
     */
    private RevenueStats revenueStats;

    /**
     * Thống kê đơn hàng theo trạng thái
     */
    private OrderStatistics orderStatistics;

    /**
     * Top sản phẩm bán chạy
     */
    private List<BestSellingProduct> bestSellingProducts;

    /**
     * Đơn hàng gần đây của shop
     */
    private List<RecentOrderInfo> recentOrders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShopInfo {
        private String shopId;
        private String shopName;
        private String logoUrl;
        private Double rating;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueStats {
        /**
         * Tổng doanh thu trong khoảng thời gian
         */
        private Double totalRevenue;

        /**
         * Ngày bắt đầu
         */
        private LocalDate startDate;

        /**
         * Ngày kết thúc
         */
        private LocalDate endDate;

        /**
         * Số đơn hàng hoàn thành
         */
        private Long completedOrdersCount;

        /**
         * Giá trị đơn hàng trung bình
         */
        private Double averageOrderValue;

        /**
         * So sánh với kỳ trước (phần trăm tăng/giảm)
         */
        private Double revenueGrowthPercent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderStatistics {
        /**
         * Tổng số đơn hàng
         */
        private Long totalOrders;

        /**
         * Số đơn chờ xác nhận
         */
        private Long pendingOrders;

        /**
         * Số đơn đã xác nhận
         */
        private Long confirmedOrders;

        /**
         * Số đơn đang vận chuyển
         */
        private Long shippingOrders;

        /**
         * Số đơn đã giao
         */
        private Long deliveredOrders;

        /**
         * Số đơn đã hủy
         */
        private Long cancelledOrders;

        /**
         * Chi tiết theo từng trạng thái
         */
        private Map<String, Long> ordersByStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BestSellingProduct {
        /**
         * ID sản phẩm
         */
        private String productId;

        /**
         * Tên sản phẩm
         */
        private String productName;

        /**
         * URL hình ảnh
         */
        private String productImage;

        /**
         * Tổng số lượng đã bán
         */
        private Long totalQuantitySold;

        /**
         * Tổng doanh thu từ sản phẩm này
         */
        private Double totalRevenue;

        /**
         * Thứ hạng
         */
        private Integer rank;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentOrderInfo {
        private String orderId;
        private String customerName;
        private Double totalAmount;
        private String status;
        private String createdAt;
    }
}
