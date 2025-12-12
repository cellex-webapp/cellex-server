package com.example.cellex.dtos.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO Response cho Admin Dashboard
 * Chứa các thông tin thống kê toàn hệ thống
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminDashboardResponse {

    /**
     * Tổng quan hệ thống
     */
    private SystemOverview systemOverview;

    /**
     * Thống kê doanh thu hệ thống
     */
    private SystemRevenueStats revenueStats;

    /**
     * Thống kê đơn hàng toàn hệ thống
     */
    private SystemOrderStats orderStats;

    /**
     * Đơn hàng gần đây nhất
     */
    private List<RecentOrder> recentOrders;

    /**
     * Top cửa hàng có doanh thu cao nhất
     */
    private List<TopShop> topShops;

    /**
     * Thống kê người dùng
     */
    private UserStats userStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemOverview {
        /**
         * Tổng số cửa hàng đang hoạt động (status = APPROVED)
         */
        private Long totalActiveShops;

        /**
         * Tổng số cửa hàng chờ duyệt
         */
        private Long totalPendingShops;

        /**
         * Tổng số người dùng
         */
        private Long totalUsers;

        /**
         * Tổng số đơn hàng toàn hệ thống
         */
        private Long totalOrders;

        /**
         * Tổng doanh thu hệ thống (từ đơn DELIVERED và đã thanh toán)
         */
        private Double totalSystemRevenue;

        /**
         * Tổng số sản phẩm
         */
        private Long totalProducts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemRevenueStats {
        /**
         * Doanh thu hôm nay
         */
        private Double todayRevenue;

        /**
         * Doanh thu tuần này
         */
        private Double thisWeekRevenue;

        /**
         * Doanh thu tháng này
         */
        private Double thisMonthRevenue;

        /**
         * Doanh thu năm nay
         */
        private Double thisYearRevenue;

        /**
         * Tăng trưởng so với tháng trước (%)
         */
        private Double monthOverMonthGrowth;

        /**
         * Doanh thu theo ngày trong tuần/tháng
         */
        private Map<String, Double> revenueByDay;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemOrderStats {
        /**
         * Tổng số đơn hàng
         */
        private Long totalOrders;

        /**
         * Phân bổ theo trạng thái
         */
        private Map<String, Long> ordersByStatus;

        /**
         * Số đơn hôm nay
         */
        private Long todayOrders;

        /**
         * Số đơn tuần này
         */
        private Long thisWeekOrders;

        /**
         * Số đơn tháng này
         */
        private Long thisMonthOrders;

        /**
         * Tỷ lệ đơn hủy (%)
         */
        private Double cancellationRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentOrder {
        /**
         * ID đơn hàng
         */
        private String orderId;

        /**
         * ID người dùng
         */
        private String userId;

        /**
         * Tên người dùng
         */
        private String userName;

        /**
         * ID cửa hàng
         */
        private String shopId;

        /**
         * Tên cửa hàng
         */
        private String shopName;

        /**
         * Tổng tiền
         */
        private Double totalAmount;

        /**
         * Trạng thái đơn hàng
         */
        private String status;

        /**
         * Phương thức thanh toán
         */
        private String paymentMethod;

        /**
         * Đã thanh toán chưa
         */
        private Boolean isPaid;

        /**
         * Thời gian tạo
         */
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopShop {
        /**
         * ID cửa hàng
         */
        private String shopId;

        /**
         * Tên cửa hàng
         */
        private String shopName;

        /**
         * Logo URL
         */
        private String logoUrl;

        /**
         * Tổng doanh thu
         */
        private Double totalRevenue;

        /**
         * Tổng số đơn hoàn thành
         */
        private Long totalCompletedOrders;

        /**
         * Thứ hạng
         */
        private Integer rank;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStats {
        /**
         * Tổng số người dùng
         */
        private Long totalUsers;

        /**
         * Số người dùng mới trong tháng
         */
        private Long newUsersThisMonth;

        /**
         * Số vendor
         */
        private Long totalVendors;

        /**
         * Số admin
         */
        private Long totalAdmins;

        /**
         * Phân bổ theo role
         */
        private Map<String, Long> usersByRole;
    }
}
