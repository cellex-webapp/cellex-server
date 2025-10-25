package com.example.cellex.controllers;

import com.example.cellex.dtos.response.UserCouponResponse;
import com.example.cellex.models.User;
import com.example.cellex.services.UserCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-coupons")
@RequiredArgsConstructor
@Tag(name = "06. User Coupons", description = "API quản lý coupon của người dùng")
public class UserCouponController {

    private final UserCouponService userCouponService;

    @GetMapping("/my-coupons")
    @Operation(summary = "Lấy danh sách coupon của tôi")
    public ResponseEntity<List<UserCouponResponse>> getMyCoupons(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(userCouponService.getUserCoupons(user.getId()));
    }

    @GetMapping("/my-active-coupons")
    @Operation(summary = "Lấy danh sách coupon đang hoạt động của tôi")
    public ResponseEntity<List<UserCouponResponse>> getMyActiveCoupons(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(userCouponService.getUserActiveCoupons(user.getId()));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy danh sách coupon của user (ADMIN)")
    public ResponseEntity<List<UserCouponResponse>> getUserCoupons(
            @PathVariable String userId
    ) {
        return ResponseEntity.ok(userCouponService.getUserCoupons(userId));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Sử dụng coupon cho đơn hàng (chua làm xong, sau làm order rồi làm tiếp)")
    public ResponseEntity<UserCouponResponse> redeemCoupon(
            @RequestParam String code,
            @RequestParam String orderId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(userCouponService.redeemCoupon(code, orderId));
    }
}

