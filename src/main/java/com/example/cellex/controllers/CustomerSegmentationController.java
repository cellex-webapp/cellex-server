package com.example.cellex.controllers;

import com.example.cellex.dtos.request.UpdateUserSpendRequest;
import com.example.cellex.services.CustomerSegmentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer-segmentation")
@RequiredArgsConstructor
@Tag(name = "07. Customer Segmentation Operations", description = "API quản lý phân khúc và nâng hạng khách hàng")
public class CustomerSegmentationController {

    private final CustomerSegmentationService customerSegmentationService;

    @PostMapping("/update-spend")
    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDOR')")
    @Operation(summary = "Cập nhật tổng chi tiêu của user", 
               description = "Cập nhật tổng chi tiêu và tự động nâng hạng nếu đạt điều kiện")
    public ResponseEntity<String> updateUserSpend(
            @Valid @RequestBody UpdateUserSpendRequest request
    ) {
        customerSegmentationService.updateUserSpend(request.getUserId(), request.getAmount());
        return ResponseEntity.ok("Đã cập nhật tổng chi tiêu và kiểm tra nâng hạng");
    }

    @PostMapping("/trigger-scheduled-coupons")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kích hoạt phát coupon theo lịch (MANUAL)", 
               description = "Chỉ dùng để test, thông thường sẽ chạy tự động bằng scheduler")
    public ResponseEntity<String> triggerScheduledCoupons() {
        customerSegmentationService.issueScheduledCoupons();
        return ResponseEntity.ok("Đã kiểm tra và phát các coupon theo lịch");
    }
}

