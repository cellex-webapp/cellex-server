package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.coupon.UserCouponResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.services.coupon.UserCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-coupons")
@RequiredArgsConstructor
@Tag(name = "06. User Coupons", description = "API quản lý coupon của người dùng")
public class UserCouponController {

    private final UserCouponService userCouponService;

    @GetMapping("/my-coupons")
    @Operation(summary = "Lấy danh sách coupon của tôi")
    public ResponseEntity<ApiResponse<com.example.cellex.dtos.response.PageResponse<UserCouponResponse>>> getMyCoupons(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "issuedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortType
    ) {
        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));

        com.example.cellex.dtos.response.PageResponse<UserCouponResponse> pageResp = userCouponService.getUserCoupons(user.getId(), pageable);
        ApiResponse<com.example.cellex.dtos.response.PageResponse<UserCouponResponse>> response = ApiResponse.<com.example.cellex.dtos.response.PageResponse<UserCouponResponse>>builder()
                .code(200)
                .message("Lấy danh sách coupon thành công")
                .result(pageResp)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-active-coupons")
    @Operation(summary = "Lấy danh sách coupon đang hoạt động của tôi")
    public ResponseEntity<ApiResponse<com.example.cellex.dtos.response.PageResponse<UserCouponResponse>>> getMyActiveCoupons(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "issuedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortType
    ) {
        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));

        com.example.cellex.dtos.response.PageResponse<UserCouponResponse> pageResp = userCouponService.getUserActiveCoupons(user.getId(), pageable);
        ApiResponse<com.example.cellex.dtos.response.PageResponse<UserCouponResponse>> response = ApiResponse.<com.example.cellex.dtos.response.PageResponse<UserCouponResponse>>builder()
                .code(200)
                .message("Lấy danh sách coupon đang hoạt động thành công")
                .result(pageResp)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy danh sách coupon của user (ADMIN)")
    public ResponseEntity<ApiResponse<com.example.cellex.dtos.response.PageResponse<UserCouponResponse>>> getUserCoupons(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "issuedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortType
    ) {
        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));

        com.example.cellex.dtos.response.PageResponse<UserCouponResponse> pageResp = userCouponService.getUserCoupons(userId, pageable);
        ApiResponse<com.example.cellex.dtos.response.PageResponse<UserCouponResponse>> response = ApiResponse.<com.example.cellex.dtos.response.PageResponse<UserCouponResponse>>builder()
                .code(200)
                .message("Lấy danh sách coupon của user thành công")
                .result(pageResp)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/redeem")
    @Operation(summary = "Sử dụng coupon cho đơn hàng (chua làm xong, sau làm order rồi làm tiếp)")
    public ResponseEntity<ApiResponse<UserCouponResponse>> redeemCoupon(
            @RequestParam String code,
            @RequestParam String orderId,
            @AuthenticationPrincipal User user
    ) {
        UserCouponResponse coupon = userCouponService.redeemCoupon(code, orderId);
        ApiResponse<UserCouponResponse> response = ApiResponse.<UserCouponResponse>builder()
                .code(200)
                .message("Sử dụng coupon thành công")
                .result(coupon)
                .build();
        return ResponseEntity.ok(response);
    }
}
