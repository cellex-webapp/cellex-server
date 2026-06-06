package com.example.cellex.controllers;

import com.example.cellex.dtos.request.warranty.ClaimStatusUpdateRequest;
import com.example.cellex.dtos.request.warranty.WarrantyClaimRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.warranty.WarrantyClaimResponse;
import com.example.cellex.enums.WarrantyStatus;
import com.example.cellex.models.user.User;
import com.example.cellex.models.warranty.WarrantyClaim;
import com.example.cellex.models.warranty.WarrantyPolicy;
import com.example.cellex.services.warranty.WarrantyService;
import com.example.cellex.utils.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warranties")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Warranty - Client", description = "Customer Warranty APIs")
public class ClientWarrantyController {

    private final WarrantyService warrantyService;

    @GetMapping("/products/{productId}/policy")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Xem chính sách bảo hành của một sản phẩm")
    public ResponseEntity<ApiResponse<WarrantyPolicy>> getProductPolicy(@PathVariable String productId) {
        WarrantyPolicy policy = warrantyService.getPolicyByProductId(productId);
        return ResponseEntity.ok(ApiResponse.<WarrantyPolicy>builder()
                .code(200).message("Thành công").result(policy).build());
    }

    @PostMapping("/claims")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo yêu cầu bảo hành mới")
    public ResponseEntity<ApiResponse<WarrantyClaim>> createClaim(
            Authentication authentication,
            @RequestBody @Valid WarrantyClaimRequest request) {

        User user = (User) authentication.getPrincipal();
        UUID userId = UUID.fromString(user.getId());
        WarrantyClaim claim = warrantyService.createClaim(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<WarrantyClaim>builder()
                        .code(201).message("Tạo yêu cầu thành công").result(claim).build());
    }

    @GetMapping("/my-claims")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xem danh sách phiếu bảo hành của tôi")
    public ResponseEntity<ApiResponse<List<WarrantyClaimResponse>>> getMyClaims(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        UUID userId = UUID.fromString(user.getId());
        List<WarrantyClaimResponse> claims = warrantyService.getUserClaims(userId);

        return ResponseEntity.ok(ApiResponse.<List<WarrantyClaimResponse>>builder()
                .code(200).message("Thành công").result(claims).build());
    }

    // ==================== VENDOR ENDPOINTS ====================

    @GetMapping("/shop/claims")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[VENDOR] Lấy danh sách yêu cầu bảo hành của shop")
    public ResponseEntity<ApiResponse<PageResponse<WarrantyClaimResponse>>> getShopClaims(
            Authentication authentication,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "Lọc theo trạng thái") @RequestParam(required = false) String status,
            @Parameter(description = "Sắp xếp theo trường") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Hướng sắp xếp (asc/desc)") @RequestParam(defaultValue = "desc") String sortType) {

        User user = (User) authentication.getPrincipal();
        String vendorId = user.getId();

        // Parse status param — null means "all"
        WarrantyStatus statusFilter = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                statusFilter = WarrantyStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid warranty status filter: {}", status);
                // ignore invalid status, treat as ALL
            }
        }

        Pageable pageable = PaginationUtil.createPageable(page, limit, sortBy, sortType);
        PageResponse<WarrantyClaimResponse> response = warrantyService.getShopClaimsForVendor(vendorId, pageable, statusFilter);

        return ResponseEntity.ok(ApiResponse.<PageResponse<WarrantyClaimResponse>>builder()
                .code(200).message("Thành công").result(response).build());
    }

    @PutMapping("/claims/{claimId}/respond")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[VENDOR] Phản hồi yêu cầu bảo hành")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> respondToClaim(
            Authentication authentication,
            @Parameter(description = "ID phiếu bảo hành") @PathVariable UUID claimId,
            @RequestBody @Valid ClaimStatusUpdateRequest request) {

        User user = (User) authentication.getPrincipal();
        String vendorId = user.getId();

        WarrantyClaimResponse response = warrantyService.respondToClaim(vendorId, claimId, request);

        return ResponseEntity.ok(ApiResponse.<WarrantyClaimResponse>builder()
                .code(200).message("Cập nhật trạng thái thành công").result(response).build());
    }
}