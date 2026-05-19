package com.example.cellex.controllers;

import com.example.cellex.dtos.request.livestream.AddProductToLiveBagRequest;
import com.example.cellex.dtos.request.livestream.CreateLivestreamRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.livestream.LivestreamProductResponse;
import com.example.cellex.dtos.response.livestream.LivestreamSessionResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.user.User;
import com.example.cellex.services.livestream.LivestreamEventPublisher;
import com.example.cellex.services.livestream.LivestreamService;
import com.example.cellex.services.staff.StaffPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestream")
@RequiredArgsConstructor
public class LivestreamController {

    private final LivestreamService livestreamService;
    private final LivestreamEventPublisher eventPublisher;
    private final StaffPermissionService staffPermissionService;

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    public ApiResponse<LivestreamSessionResponse> createSession(
            Authentication authentication,
            @Valid @RequestBody CreateLivestreamRequest request) {
        User operator = (User) authentication.getPrincipal();
        if (operator.getRole() == Role.STAFF && !staffPermissionService.hasPermission(operator.getId(), "LIVESTREAM:CREATE")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        return ApiResponse.<LivestreamSessionResponse>builder()
                .result(livestreamService.createSession(operator, request))
                .message("Tao phien Live thanh cong")
                .build();
    }

    @PutMapping("/sessions/{id}/end")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    public ApiResponse<Void> endSession(
            @PathVariable String id,
            Authentication authentication) {
        User operator = (User) authentication.getPrincipal();
        if (operator.getRole() == Role.STAFF && !staffPermissionService.hasPermission(operator.getId(), "LIVESTREAM:MANAGE")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        livestreamService.endSession(id, operator);
        return ApiResponse.<Void>builder()
                .message("Ket thuc phien Live thanh cong")
                .build();
    }

    @GetMapping("/sessions/active")
    public ApiResponse<List<LivestreamSessionResponse>> getActiveSessions() {
        return ApiResponse.<List<LivestreamSessionResponse>>builder()
                .result(livestreamService.getActiveSessions())
                .build();
    }

    @GetMapping("/sessions/{id}/viewer-token")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'ADMIN', 'STAFF')")
    public ApiResponse<String> getViewerToken(
            @PathVariable String id,
            Authentication authentication) {
        User viewer = (User) authentication.getPrincipal();
        return ApiResponse.<String>builder()
                .result(livestreamService.getViewerToken(id, viewer))
                .build();
    }

    @PostMapping("/sessions/{id}/products")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    public ApiResponse<Void> addProductToBag(
            @PathVariable String id,
            @Valid @RequestBody AddProductToLiveBagRequest request,
            Authentication authentication) {
        User operator = (User) authentication.getPrincipal();
        if (operator.getRole() == Role.STAFF && !staffPermissionService.hasPermission(operator.getId(), "LIVESTREAM:MANAGE")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        livestreamService.addProductToBag(id, request, operator);
        return ApiResponse.<Void>builder()
                .message("Da them san pham vao tui hang")
                .build();
    }

    @GetMapping("/sessions/{id}/products")
    public ApiResponse<List<LivestreamProductResponse>> getSessionProducts(@PathVariable String id) {
        return ApiResponse.<List<LivestreamProductResponse>>builder()
                .result(livestreamService.getSessionProducts(id))
                .build();
    }

    @PostMapping("/sessions/{id}/products/{productId}/pin")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    public ApiResponse<Void> pinProduct(
            @PathVariable String id,
            @PathVariable String productId,
            Authentication authentication) {
        User operator = (User) authentication.getPrincipal();
        if (operator.getRole() == Role.STAFF && !staffPermissionService.hasPermission(operator.getId(), "LIVESTREAM:MANAGE")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        eventPublisher.broadcastPinProduct(id, productId);
        return ApiResponse.<Void>builder()
                .message("Da ghim san pham len man hinh")
                .build();
    }
}
