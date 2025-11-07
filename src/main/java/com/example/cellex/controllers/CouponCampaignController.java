package com.example.cellex.controllers;

import com.example.cellex.dtos.request.coupon.CreateCampaignRequest;
import com.example.cellex.dtos.request.coupon.DistributeCampaignRequest;
import com.example.cellex.dtos.request.coupon.UpdateCampaignRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.coupon.CampaignDistributionResponse;
import com.example.cellex.dtos.response.coupon.CouponCampaignResponse;
import com.example.cellex.enums.CampaignStatus;
import com.example.cellex.models.user.User;
import com.example.cellex.services.coupon.CouponCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupon-campaigns")
@RequiredArgsConstructor
@Tag(name = "12. Coupon Campaigns")
public class CouponCampaignController {

    private final CouponCampaignService campaignService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Tạo campaign coupon mới",
        description = """
            Tạo campaign coupon với cấu hình đầy đủ. **Chỉ ADMIN có quyền.**
            
            **Các loại coupon:**
            - PERCENTAGE: Giảm theo % (discountValue <= 100)
            - FIXED: Giảm số tiền cố định
            - FREE_SHIPPING: Miễn phí vận chuyển
            
            **Distribution Type:**
            - SHARED_CODE: Tất cả dùng chung 1 code (codeTemplate required)
            - UNIQUE_PER_USER: Mỗi user 1 code riêng (auto-generate)
            
            **Scheduling:**
            - scheduledAt = null: Campaign ở trạng thái DRAFT, cần phát thủ công
            - scheduledAt = datetime: Campaign tự động phát vào thời điểm đó
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN")
        }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Thông tin campaign",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "SHARED_CODE - Flash Sale",
                    summary = "Coupon dùng chung cho Flash Sale",
                    value = """
                        {
                          "title": "Flash Sale Tết 2025",
                          "description": "Giảm 20% cho mọi đơn hàng",
                          "codeTemplate": "TET2025",
                          "couponType": "PERCENTAGE",
                          "discountValue": 20,
                          "minOrderAmount": 500000,
                          "startDate": "2025-01-20T00:00:00",
                          "endDate": "2025-01-31T23:59:59",
                          "distributionType": "SHARED_CODE",
                          "maxTotalIssuance": 1000,
                          "perUserLimit": 1
                        }
                        """
                ),
                @ExampleObject(
                    name = "UNIQUE - VIP Members",
                    summary = "Coupon riêng cho từng VIP member",
                    value = """
                        {
                          "title": "VIP Exclusive Coupon",
                          "description": "Giảm 500K cho VIP members",
                          "couponType": "FIXED",
                          "discountValue": 500000,
                          "minOrderAmount": 2000000,
                          "applicableProductIds": ["prod1", "prod2"],
                          "startDate": "2025-02-01T00:00:00",
                          "endDate": "2025-02-28T23:59:59",
                          "distributionType": "UNIQUE_PER_USER",
                          "perUserLimit": 1,
                          "note": "Chỉ áp dụng cho sản phẩm cao cấp"
                        }
                        """
                )
            }
        )
    )
    public ResponseEntity<ApiResponse<CouponCampaignResponse>> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            @AuthenticationPrincipal User admin
    ) {
        CouponCampaignResponse campaign = campaignService.createCampaign(request, admin.getId());
        ApiResponse<CouponCampaignResponse> response = ApiResponse.<CouponCampaignResponse>builder()
                .code(200)
                .message("Tạo campaign thành công")
                .result(campaign)
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Cập nhật campaign",
        description = """
            Cập nhật thông tin campaign. **Chỉ ADMIN có quyền.**
            
            ⚠️ **Lưu ý:**
            - Không thể cập nhật campaign đã phát (status = ACTIVE hoặc COMPLETED)
            - Tất cả trường đều optional
            """
    )
    public ResponseEntity<ApiResponse<CouponCampaignResponse>> updateCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable String id,
            @Valid @RequestBody UpdateCampaignRequest request
    ) {
        CouponCampaignResponse campaign = campaignService.updateCampaign(id, request);
        ApiResponse<CouponCampaignResponse> response = ApiResponse.<CouponCampaignResponse>builder()
                .code(200)
                .message("Cập nhật campaign thành công")
                .result(campaign)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Xóa campaign",
        description = """
            Xóa campaign. **Chỉ ADMIN có quyền.**
            
            ⚠️ Không thể xóa campaign đang chạy (status = ACTIVE)
            """
    )
    public ResponseEntity<ApiResponse<String>> deleteCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable String id
    ) {
        campaignService.deleteCampaign(id);
        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(200)
                .message("Xóa campaign thành công")
                .result("Đã xóa")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thông tin campaign theo ID")
    public ResponseEntity<ApiResponse<CouponCampaignResponse>> getCampaignById(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable String id
    ) {
        CouponCampaignResponse campaign = campaignService.getCampaignById(id);
        ApiResponse<CouponCampaignResponse> response = ApiResponse.<CouponCampaignResponse>builder()
                .code(200)
                .message("Lấy thông tin campaign thành công")
                .result(campaign)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Lấy danh sách tất cả campaigns",
        description = "Lấy tất cả campaigns active, sắp xếp theo ngày tạo (mới nhất trước)"
    )
    public ResponseEntity<ApiResponse<List<CouponCampaignResponse>>> getAllCampaigns() {
        List<CouponCampaignResponse> campaigns = campaignService.getAllCampaigns();
        ApiResponse<List<CouponCampaignResponse>> response = ApiResponse.<List<CouponCampaignResponse>>builder()
                .code(200)
                .message("Lấy danh sách campaign thành công")
                .result(campaigns)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Lấy campaigns theo status",
        description = """
            Filter campaigns theo trạng thái:
            - DRAFT: Nháp, chưa phát
            - SCHEDULED: Đã lên lịch
            - ACTIVE: Đang chạy
            - COMPLETED: Đã hoàn thành
            - CANCELLED: Đã hủy
            """
    )
    public ResponseEntity<ApiResponse<List<CouponCampaignResponse>>> getCampaignsByStatus(
            @Parameter(description = "Campaign status", required = true)
            @PathVariable CampaignStatus status
    ) {
        List<CouponCampaignResponse> campaigns = campaignService.getCampaignsByStatus(status);
        ApiResponse<List<CouponCampaignResponse>> response = ApiResponse.<List<CouponCampaignResponse>>builder()
                .code(200)
                .message("Lấy danh sách campaign theo status thành công")
                .result(campaigns)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/distribute")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Phát coupon campaign cho users",
        description = """
            Phát coupon cho users dựa trên filter criteria. **Chỉ ADMIN có quyền.**
            
            **Filter options (AND logic):**
            - all: true → Phát cho tất cả users active
            - customerSegmentId → Filter theo segment
            - minTotalSpend, maxTotalSpend → Filter theo tổng chi tiêu
            - registeredBefore, registeredAfter → Filter theo ngày đăng ký
            - city, district → Filter theo địa điểm
            - explicitUserIds → Chỉ phát cho user IDs cụ thể
            - excludeUserIds → Loại trừ user IDs
            
            **Tránh trùng lặp:**
            - Hệ thống tự động check perUserLimit
            - Không phát nếu user đã nhận đủ số lần
            
            **Response:** Báo cáo chi tiết số lượng thành công/thất bại
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Phát thành công"),
            @ApiResponse(responseCode = "400", description = "Campaign không hợp lệ")
        }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Campaign ID và filter criteria",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "Phát cho tất cả users",
                    value = """
                        {
                          "campaignId": "campaign_id_here",
                          "filter": {
                            "all": true
                          }
                        }
                        """
                ),
                @ExampleObject(
                    name = "Phát cho segment Gold",
                    value = """
                        {
                          "campaignId": "campaign_id_here",
                          "filter": {
                            "customerSegmentId": "gold_segment_id"
                          }
                        }
                        """
                ),
                @ExampleObject(
                    name = "Phát cho users chi tiêu cao",
                    value = """
                        {
                          "campaignId": "campaign_id_here",
                          "filter": {
                            "minTotalSpend": 10000000
                          }
                        }
                        """
                ),
                @ExampleObject(
                    name = "Phát cho users cụ thể",
                    value = """
                        {
                          "campaignId": "campaign_id_here",
                          "filter": {
                            "explicitUserIds": ["user1", "user2", "user3"]
                          }
                        }
                        """
                ),
                @ExampleObject(
                    name = "Filter kết hợp",
                    value = """
                        {
                          "campaignId": "campaign_id_here",
                          "filter": {
                            "customerSegmentId": "gold_segment_id",
                            "minTotalSpend": 5000000,
                            "city": "Hà Nội",
                            "excludeUserIds": ["user_already_received"]
                          }
                        }
                        """
                )
            }
        )
    )
    public ResponseEntity<ApiResponse<CampaignDistributionResponse>> distributeCampaign(
            @Valid @RequestBody DistributeCampaignRequest request,
            @AuthenticationPrincipal User admin
    ) {
        CampaignDistributionResponse distribution = campaignService.distributeCampaign(
            request.getCampaignId(), request.getFilter(), admin.getId()
        );
        ApiResponse<CampaignDistributionResponse> response = ApiResponse.<CampaignDistributionResponse>builder()
                .code(200)
                .message("Phát campaign thành công")
                .result(distribution)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/distribution-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Xem lịch sử phát coupon của campaign",
        description = "Lấy tất cả distribution logs của campaign để audit và báo cáo"
    )
    public ResponseEntity<ApiResponse<List<CampaignDistributionResponse>>> getCampaignDistributionLogs(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable String id
    ) {
        List<CampaignDistributionResponse> logs = campaignService.getCampaignDistributionLogs(id);
        ApiResponse<List<CampaignDistributionResponse>> response = ApiResponse.<List<CampaignDistributionResponse>>builder()
                .code(200)
                .message("Lấy lịch sử phát campaign thành công")
                .result(logs)
                .build();
        return ResponseEntity.ok(response);
    }
}

