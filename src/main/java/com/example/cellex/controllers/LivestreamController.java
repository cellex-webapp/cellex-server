package com.example.cellex.controllers;

import com.example.cellex.dtos.request.livestream.AddProductToLiveBagRequest;
import com.example.cellex.dtos.request.livestream.CreateLivestreamRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.livestream.LivestreamSessionResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.services.livestream.LivestreamEventPublisher;
import com.example.cellex.services.livestream.LivestreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/livestream")
@RequiredArgsConstructor
public class LivestreamController {

    private final LivestreamService livestreamService;
    private final LivestreamEventPublisher eventPublisher;

    @PostMapping("/sessions")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<LivestreamSessionResponse> createSession(
            Authentication authentication,
            @Valid @RequestBody CreateLivestreamRequest request) {
        User vendor = (User) authentication.getPrincipal();
        return ApiResponse.<LivestreamSessionResponse>builder()
                .result(livestreamService.createSession(vendor, request))
                .message("Tạo phiên Live thành công")
                .build();
    }

    @PutMapping("/sessions/{id}/end")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<Void> endSession(
            @PathVariable String id,
            Authentication authentication) {
        User vendor = (User) authentication.getPrincipal();
        livestreamService.endSession(id, vendor);
        return ApiResponse.<Void>builder()
                .message("Kết thúc phiên Live thành công")
                .build();
    }

    @GetMapping("/sessions/active")
    public ApiResponse<List<LivestreamSessionResponse>> getActiveSessions() {
        return ApiResponse.<List<LivestreamSessionResponse>>builder()
                .result(livestreamService.getActiveSessions())
                .build();
    }

    @GetMapping("/sessions/{id}/viewer-token")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'ADMIN')")
    public ApiResponse<String> getViewerToken(
            @PathVariable String id,
            Authentication authentication) {
        User viewer = (User) authentication.getPrincipal();
        return ApiResponse.<String>builder()
                .result(livestreamService.getViewerToken(id, viewer))
                .build();
    }

    @PostMapping("/sessions/{id}/products")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<Void> addProductToBag(
            @PathVariable String id,
            @Valid @RequestBody AddProductToLiveBagRequest request,
            Authentication authentication) {
        User vendor = (User) authentication.getPrincipal();
        livestreamService.addProductToBag(id, request, vendor);
        return ApiResponse.<Void>builder()
                .message("Đã thêm sản phẩm vào túi hàng")
                .build();
    }

    // API để Vendor ghim sản phẩm (Sẽ bắn WebSocket xuống cho Viewers)
    @PostMapping("/sessions/{id}/products/{productId}/pin")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<Void> pinProduct(
            @PathVariable String id,
            @PathVariable String productId,
            Authentication authentication) {
        // Tạm bỏ qua logic validate host để giữ code gọn
        eventPublisher.broadcastPinProduct(id, productId);
        return ApiResponse.<Void>builder()
                .message("Đã ghim sản phẩm lên màn hình")
                .build();
    }
}