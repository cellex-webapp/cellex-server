package com.example.cellex.controllers;

import com.example.cellex.dtos.request.user.UpdateUserSpendRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.services.segment.CustomerSegmentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer-segmentation")
@RequiredArgsConstructor
@Tag(name = "07. Customer Segmentation Operations", description = "API quản lý phân khúc và nâng hạng khách hàng")
public class CustomerSegmentationController {

    private final CustomerSegmentationService customerSegmentationService;

    @PostMapping("/update-spend")
    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDOR')")
    @Operation(summary = "Cập nhật tổng chi tiêu của user", 
               description = "Cập nhật tổng chi tiêu và tự động nâng hạng nếu đạt điều kiện")
    public ResponseEntity<ApiResponse<String>> updateUserSpend(
            @Valid @RequestBody UpdateUserSpendRequest request
    ) {
        customerSegmentationService.updateUserSpend(request.getUserId(), request.getAmount());

        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(200)
                .message("Đã cập nhật tổng chi tiêu và kiểm tra nâng hạng thành công")
                .result("Cập nhật thành công")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/trigger-scheduled-coupons")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kích hoạt phát coupon theo lịch (MANUAL)", 
               description = "Chỉ dùng để test, thông thường sẽ chạy tự động bằng scheduler")
    public ResponseEntity<ApiResponse<String>> triggerScheduledCoupons() {
        customerSegmentationService.issueScheduledCoupons();

        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(200)
                .message("Đã kiểm tra và phát các coupon theo lịch thành công")
                .result("Phát coupon thành công")
                .build();

        return ResponseEntity.ok(response);
    }
}
