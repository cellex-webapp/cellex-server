package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.analytics.AdminDashboardResponse;
import com.example.cellex.dtos.response.analytics.AdminDashboardResponse.*;
import com.example.cellex.dtos.response.analytics.VendorDashboardResponse;
import com.example.cellex.dtos.response.analytics.VendorDashboardResponse.*;
import com.example.cellex.models.user.User;
import com.example.cellex.services.analytics.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller xử lý các API Business Analytics & Reporting
 * 
 * Vendor APIs:
 * - GET /api/v1/analytics/vendor/dashboard: Dashboard đầy đủ của vendor
 * - GET /api/v1/analytics/vendor/revenue: Thống kê doanh thu
 * - GET /api/v1/analytics/vendor/orders: Thống kê đơn hàng
 * - GET /api/v1/analytics/vendor/best-selling: Sản phẩm bán chạy
 * 
 * Admin APIs:
 * - GET /api/v1/analytics/admin/dashboard: Dashboard đầy đủ của admin
 * - GET /api/v1/analytics/admin/overview: Tổng quan hệ thống
 * - GET /api/v1/analytics/admin/recent-orders: Đơn hàng gần đây
 * - GET /api/v1/analytics/admin/top-shops: Top cửa hàng
 * 
 * @author Cellex Team
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "APIs cho Business Analytics & Reporting")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ==================== VENDOR ENDPOINTS ====================

    /**
     * Lấy dashboard đầy đủ cho Vendor
     */
    @GetMapping("/vendor/dashboard")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Lấy Vendor Dashboard", 
               description = "Lấy toàn bộ thông tin thống kê của cửa hàng. Vendor chỉ được xem shop của mình.")
    public ResponseEntity<ApiResponse<VendorDashboardResponse>> getVendorDashboard(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID của shop") @RequestParam String shopId,
            @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Getting vendor dashboard for shop: {} by user: {}", shopId, currentUser.getId());
        
        VendorDashboardResponse dashboard = analyticsService.getVendorDashboard(
                shopId, currentUser, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<VendorDashboardResponse>builder()
                .code(200)
                .message("Lấy thống kê cửa hàng thành công")
                .result(dashboard)
                .build());
    }

    /**
     * Lấy thống kê doanh thu của shop
     */
    @GetMapping("/vendor/revenue")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Lấy thống kê doanh thu", 
               description = "Lấy thống kê doanh thu của cửa hàng trong khoảng thời gian")
    public ResponseEntity<ApiResponse<RevenueStats>> getVendorRevenue(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID của shop") @RequestParam String shopId,
            @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD)", required = true) 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (YYYY-MM-DD)", required = true) 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // Validate access
        analyticsService.getVendorDashboard(shopId, currentUser, null, null); // Just for validation
        
        RevenueStats revenueStats = analyticsService.getVendorRevenue(shopId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<RevenueStats>builder()
                .code(200)
                .message("Lấy thống kê doanh thu thành công")
                .result(revenueStats)
                .build());
    }

    /**
     * Lấy thống kê đơn hàng theo trạng thái
     */
    @GetMapping("/vendor/orders")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Lấy thống kê đơn hàng", 
               description = "Lấy thống kê số đơn hàng theo từng trạng thái")
    public ResponseEntity<ApiResponse<OrderStatistics>> getOrderStatistics(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID của shop") @RequestParam String shopId
    ) {
        // Validate access
        analyticsService.getVendorDashboard(shopId, currentUser, null, null); // Just for validation
        
        OrderStatistics statistics = analyticsService.getOrderStatistics(shopId);

        return ResponseEntity.ok(ApiResponse.<OrderStatistics>builder()
                .code(200)
                .message("Lấy thống kê đơn hàng thành công")
                .result(statistics)
                .build());
    }

    /**
     * Lấy top sản phẩm bán chạy
     */
    @GetMapping("/vendor/best-selling")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Lấy sản phẩm bán chạy", 
               description = "Lấy top 5 sản phẩm bán chạy nhất của cửa hàng")
    public ResponseEntity<ApiResponse<List<BestSellingProduct>>> getBestSellingProducts(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID của shop") @RequestParam String shopId
    ) {
        // Validate access
        analyticsService.getVendorDashboard(shopId, currentUser, null, null); // Just for validation
        
        List<BestSellingProduct> products = analyticsService.getBestSellingProducts(shopId);

        return ResponseEntity.ok(ApiResponse.<List<BestSellingProduct>>builder()
                .code(200)
                .message("Lấy danh sách sản phẩm bán chạy thành công")
                .result(products)
                .build());
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Lấy dashboard đầy đủ cho Admin
     */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy Admin Dashboard", 
               description = "Lấy toàn bộ thông tin thống kê hệ thống. Chỉ Admin mới có quyền.")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard(
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Getting admin dashboard by user: {}", currentUser.getId());
        
        AdminDashboardResponse dashboard = analyticsService.getAdminDashboard();

        return ResponseEntity.ok(ApiResponse.<AdminDashboardResponse>builder()
                .code(200)
                .message("Lấy thống kê hệ thống thành công")
                .result(dashboard)
                .build());
    }

    /**
     * Lấy tổng quan hệ thống
     */
    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tổng quan hệ thống", 
               description = "Lấy các thông số tổng quan của hệ thống")
    public ResponseEntity<ApiResponse<SystemOverview>> getSystemOverview(
            @AuthenticationPrincipal User currentUser
    ) {
        SystemOverview overview = analyticsService.getSystemOverview();

        return ResponseEntity.ok(ApiResponse.<SystemOverview>builder()
                .code(200)
                .message("Lấy tổng quan hệ thống thành công")
                .result(overview)
                .build());
    }

    /**
     * Lấy đơn hàng gần đây nhất
     */
    @GetMapping("/admin/recent-orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy đơn hàng gần đây", 
               description = "Lấy 5 đơn hàng mới nhất của toàn hệ thống")
    public ResponseEntity<ApiResponse<List<RecentOrder>>> getRecentOrders(
            @AuthenticationPrincipal User currentUser
    ) {
        List<RecentOrder> orders = analyticsService.getRecentOrders();

        return ResponseEntity.ok(ApiResponse.<List<RecentOrder>>builder()
                .code(200)
                .message("Lấy danh sách đơn hàng gần đây thành công")
                .result(orders)
                .build());
    }

    /**
     * Lấy top cửa hàng có doanh thu cao nhất
     */
    @GetMapping("/admin/top-shops")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy top cửa hàng", 
               description = "Lấy top 5 cửa hàng có doanh thu cao nhất")
    public ResponseEntity<ApiResponse<List<TopShop>>> getTopShops(
            @AuthenticationPrincipal User currentUser
    ) {
        List<TopShop> shops = analyticsService.getTopShops();

        return ResponseEntity.ok(ApiResponse.<List<TopShop>>builder()
                .code(200)
                .message("Lấy danh sách top cửa hàng thành công")
                .result(shops)
                .build());
    }

    /**
     * Lấy thống kê doanh thu hệ thống
     */
    @GetMapping("/admin/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thống kê doanh thu hệ thống", 
               description = "Lấy thống kê doanh thu theo ngày/tuần/tháng/năm")
    public ResponseEntity<ApiResponse<SystemRevenueStats>> getSystemRevenueStats(
            @AuthenticationPrincipal User currentUser
    ) {
        SystemRevenueStats stats = analyticsService.getSystemRevenueStats();

        return ResponseEntity.ok(ApiResponse.<SystemRevenueStats>builder()
                .code(200)
                .message("Lấy thống kê doanh thu hệ thống thành công")
                .result(stats)
                .build());
    }

    /**
     * Lấy thống kê đơn hàng hệ thống
     */
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thống kê đơn hàng hệ thống", 
               description = "Lấy thống kê đơn hàng toàn hệ thống")
    public ResponseEntity<ApiResponse<SystemOrderStats>> getSystemOrderStats(
            @AuthenticationPrincipal User currentUser
    ) {
        SystemOrderStats stats = analyticsService.getSystemOrderStats();

        return ResponseEntity.ok(ApiResponse.<SystemOrderStats>builder()
                .code(200)
                .message("Lấy thống kê đơn hàng hệ thống thành công")
                .result(stats)
                .build());
    }

    /**
     * Lấy thống kê người dùng
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thống kê người dùng", 
               description = "Lấy thống kê người dùng theo role")
    public ResponseEntity<ApiResponse<UserStats>> getUserStats(
            @AuthenticationPrincipal User currentUser
    ) {
        UserStats stats = analyticsService.getUserStats();

        return ResponseEntity.ok(ApiResponse.<UserStats>builder()
                .code(200)
                .message("Lấy thống kê người dùng thành công")
                .result(stats)
                .build());
    }
}
