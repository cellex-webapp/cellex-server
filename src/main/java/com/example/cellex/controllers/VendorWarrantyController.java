package com.example.cellex.controllers;

import com.example.cellex.dtos.request.warranty.ClaimStatusUpdateRequest;
import com.example.cellex.dtos.request.warranty.WarrantyPolicyRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.models.warranty.WarrantyClaim;
import com.example.cellex.models.warranty.WarrantyPolicy;
import com.example.cellex.services.warranty.WarrantyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shops/{shopId}/warranties")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Warranty - Vendor", description = "Shop Warranty Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class VendorWarrantyController {

    private final WarrantyService warrantyService;

    @PostMapping("/policies/{productId}")
    @PreAuthorize("hasRole('VENDOR') and @shopSecurityValidator.isShopOwner(authentication, #shopId)")
    @Operation(summary = "Tạo hoặc cập nhật chính sách bảo hành cho sản phẩm")
    public ResponseEntity<ApiResponse<WarrantyPolicy>> createOrUpdatePolicy(
            @PathVariable UUID shopId,
            @PathVariable String productId,
            @RequestBody @Valid WarrantyPolicyRequest request) {
        
        // Bạn có thể thêm bước kiểm tra xem productId này có thực sự thuộc về shopId không ở tầng Service
        WarrantyPolicy policy = warrantyService.createOrUpdatePolicy(productId, request);
        
        return ResponseEntity.ok(ApiResponse.<WarrantyPolicy>builder()
                .code(200).message("Cập nhật chính sách thành công").result(policy).build());
    }

    @GetMapping("/claims")
    @PreAuthorize("hasRole('VENDOR') and @shopSecurityValidator.isShopOwner(authentication, #shopId)")
    @Operation(summary = "Xem danh sách yêu cầu bảo hành của khách hàng")
    public ResponseEntity<ApiResponse<List<WarrantyClaim>>> getShopClaims(@PathVariable UUID shopId) {
        List<WarrantyClaim> claims = warrantyService.getShopClaims(shopId);
        
        return ResponseEntity.ok(ApiResponse.<List<WarrantyClaim>>builder()
                .code(200).message("Thành công").result(claims).build());
    }

    @PutMapping("/claims/{claimId}/status")
    @PreAuthorize("hasRole('VENDOR') and @shopSecurityValidator.isShopOwner(authentication, #shopId)")
    @Operation(summary = "Cập nhật trạng thái phiếu bảo hành và phản hồi khách")
    public ResponseEntity<ApiResponse<WarrantyClaim>> updateClaimStatus(
            @PathVariable UUID shopId,
            @PathVariable UUID claimId,
            @RequestBody @Valid ClaimStatusUpdateRequest request) {
        
        WarrantyClaim claim = warrantyService.updateClaimStatus(shopId, claimId, request);
        
        return ResponseEntity.ok(ApiResponse.<WarrantyClaim>builder()
                .code(200).message("Cập nhật trạng thái thành công").result(claim).build());
    }
}