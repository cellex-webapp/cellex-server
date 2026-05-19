package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.analytics.*;
import com.example.cellex.dtos.response.analytics.VendorDashboardResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.services.analytics.*;
import com.example.cellex.services.staff.StaffPermissionService;
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

/**
 * Controller xử lý các API Business Analytics & Reporting
 * 
 * ADMIN ENDPOINTS:
 * - GET /api/v1/analytics/admin/dashboard          : Dashboard tổng quan hệ thống
 * - GET /api/v1/analytics/admin/customers          : Analytics chi tiết về khách hàng
 * - GET /api/v1/analytics/admin/products           : Analytics chi tiết về sản phẩm
 * - GET /api/v1/analytics/admin/shops              : Analytics chi tiết về cửa hàng
 * 
 * VENDOR ENDPOINTS:
 * - GET /api/v1/analytics/vendor/dashboard         : Dashboard của vendor (shop)
 * 
 * Tất cả endpoints đều hỗ trợ filter theo khoảng thời gian (startDate, endDate)
 * 
 * @author Cellex Team
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "17. Analytics", description = "APIs cho Business Analytics & Reporting")
public class AnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final CustomerAnalyticsService customerAnalyticsService;
    private final ProductAnalyticsService productAnalyticsService;
    private final ShopAnalyticsService shopAnalyticsService;
    private final AnalyticsService vendorAnalyticsService;
    private final StaffPermissionService staffPermissionService;

    // ==================== ADMIN DASHBOARD ====================

    /**
     * Admin Dashboard - Tổng quan hệ thống
     * 
     * Hiển thị:
     * - 3 Summary Cards: Tổng doanh thu, Tổng đơn hàng, Khách hàng mới
     * - Secondary KPIs: AOV, Conversion Rate, Cancellation Rate, etc.
     * - Charts: Revenue, Orders, Status Distribution, Revenue by Category
     * - Top Performers: Top Shops, Top Products, Top Customers
     * - Recent Activities: Recent Orders, New Shops, New Users
     */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Admin Dashboard - Tổng quan hệ thống",
        description = "Lấy toàn bộ số liệu tổng quan cho Admin Dashboard. " +
                      "Bao gồm summary cards, KPIs, charts, top performers và recent activities."
    )
    public ResponseEntity<ApiResponse<AdminMainDashboardResponse>> getAdminDashboard(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD). Mặc định: đầu tháng hiện tại")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (YYYY-MM-DD). Mặc định: hôm nay")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Admin {} requesting main dashboard - date range: {} to {}", 
                currentUser.getId(), startDate, endDate);
        
        AdminMainDashboardResponse dashboard = adminAnalyticsService.getAdminDashboard(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<AdminMainDashboardResponse>builder()
                .code(200)
                .message("Lấy Admin Dashboard thành công")
                .result(dashboard)
                .build());
    }

    // ==================== CUSTOMER ANALYTICS ====================

    /**
     * Customer Analytics - Số liệu chi tiết về khách hàng
     * 
     * Hiển thị:
     * - 3 Summary Cards: Tổng khách hàng, Khách hàng mới, Khách hàng hoạt động
     * - Overview: Chi tiết về return rate, ACV, orders per customer
     * - Charts: New customers, Active customers, Segments, Spending trends
     * - Customer Segments: VIP, Regular, New, Inactive
     * - Top Customers & Recent Customers
     */
    @GetMapping("/admin/customers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Customer Analytics - Số liệu khách hàng",
        description = "Lấy số liệu chi tiết về khách hàng. " +
                      "Bao gồm segments, behavior, spending patterns."
    )
    public ResponseEntity<ApiResponse<CustomerAnalyticsResponse>> getCustomerAnalytics(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD). Mặc định: đầu tháng hiện tại")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (YYYY-MM-DD). Mặc định: hôm nay")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Admin {} requesting customer analytics - date range: {} to {}", 
                currentUser.getId(), startDate, endDate);
        
        CustomerAnalyticsResponse analytics = customerAnalyticsService.getCustomerAnalytics(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<CustomerAnalyticsResponse>builder()
                .code(200)
                .message("Lấy Customer Analytics thành công")
                .result(analytics)
                .build());
    }

    // ==================== PRODUCT ANALYTICS ====================

    /**
     * Product Analytics - Số liệu chi tiết về sản phẩm
     * 
     * Hiển thị:
     * - 3 Summary Cards: Sản phẩm đang bán, Sản phẩm mới, Số lượng đã bán
     * - Overview: Stock status, ratings, revenue
     * - Charts: Sales quantity, Revenue, Category distribution
     * - Category Performance
     * - Top Products (by quantity, revenue, rating)
     * - Recent Products
     */
    @GetMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Product Analytics - Số liệu sản phẩm",
        description = "Lấy số liệu chi tiết về sản phẩm. " +
                      "Bao gồm sales, inventory, category performance."
    )
    public ResponseEntity<ApiResponse<ProductAnalyticsResponse>> getProductAnalytics(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD). Mặc định: đầu tháng hiện tại")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (YYYY-MM-DD). Mặc định: hôm nay")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Admin {} requesting product analytics - date range: {} to {}", 
                currentUser.getId(), startDate, endDate);
        
        ProductAnalyticsResponse analytics = productAnalyticsService.getProductAnalytics(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<ProductAnalyticsResponse>builder()
                .code(200)
                .message("Lấy Product Analytics thành công")
                .result(analytics)
                .build());
    }

    // ==================== SHOP ANALYTICS ====================

    /**
     * Shop Analytics - Số liệu chi tiết về cửa hàng
     * 
     * Hiển thị:
     * - 3 Summary Cards: Shop đang hoạt động, Shop mới, Tổng doanh thu
     * - Overview: Average metrics per shop
     * - Charts: New shops, Revenue, Status distribution, Rating distribution
     * - Status Distribution
     * - Top Shops (by revenue, orders, rating, products)
     * - Pending Shops (chờ duyệt)
     */
    @GetMapping("/admin/shops")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Shop Analytics - Số liệu cửa hàng",
        description = "Lấy số liệu chi tiết về cửa hàng. " +
                      "Bao gồm performance, status, top shops."
    )
    public ResponseEntity<ApiResponse<ShopAnalyticsResponse>> getShopAnalytics(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD). Mặc định: đầu tháng hiện tại")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (YYYY-MM-DD). Mặc định: hôm nay")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Admin {} requesting shop analytics - date range: {} to {}", 
                currentUser.getId(), startDate, endDate);
        
        ShopAnalyticsResponse analytics = shopAnalyticsService.getShopAnalytics(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<ShopAnalyticsResponse>builder()
                .code(200)
                .message("Lấy Shop Analytics thành công")
                .result(analytics)
                .build());
    }

    // ==================== VENDOR DASHBOARD ====================

    /**
     * Vendor Dashboard - Thống kê cho cửa hàng
     * 
     * Hiển thị:
     * - Shop Info
     * - Revenue Stats
     * - Order Statistics
     * - Best Selling Products
     * - Recent Orders
     */
    @GetMapping("/vendor/dashboard")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'STAFF')")
    @Operation(
        summary = "Vendor Dashboard - Thống kê cửa hàng",
        description = "Lấy toàn bộ thông tin thống kê của cửa hàng. " +
                      "Vendor chỉ được xem shop của mình, Admin có thể xem tất cả."
    )
    public ResponseEntity<ApiResponse<VendorDashboardResponse>> getVendorDashboard(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID của shop", required = true)
            @RequestParam String shopId,
            @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD). Mặc định: đầu tháng hiện tại")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (YYYY-MM-DD). Mặc định: hôm nay")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("User {} requesting vendor dashboard for shop {} - date range: {} to {}", 
                currentUser.getId(), shopId, startDate, endDate);
        if (currentUser.getRole() == Role.STAFF) {
            if (!staffPermissionService.hasPermission(currentUser.getId(), "ANALYTICS:VIEW")) {
                throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
            }
            String staffShopId = staffPermissionService.getStaffShopId(currentUser.getId());
            if (staffShopId == null || !staffShopId.equals(shopId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }
        
        VendorDashboardResponse dashboard = vendorAnalyticsService.getVendorDashboard(
                shopId, currentUser, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<VendorDashboardResponse>builder()
                .code(200)
                .message("Lấy Vendor Dashboard thành công")
                .result(dashboard)
                .build());
    }
}
