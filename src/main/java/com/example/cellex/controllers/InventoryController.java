package com.example.cellex.controllers;

import com.example.cellex.dtos.request.inventory.InventoryCheckBalanceRequest;
import com.example.cellex.dtos.request.inventory.InventoryImportRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.inventory.InventoryCheckHistoryResponse;
import com.example.cellex.dtos.response.inventory.InventoryCheckResponse;
import com.example.cellex.dtos.response.inventory.InventoryImportHistoryResponse;
import com.example.cellex.dtos.response.inventory.InventoryImportResponse;
import com.example.cellex.dtos.response.inventory.ProductSkuSearchResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.services.inventory.InventoryService;
import com.example.cellex.services.staff.StaffPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "15. Inventory", description = "APIs nhap kho, kiem ke va ton kho SKU")
public class InventoryController {

    private final InventoryService inventoryService;
    private final StaffPermissionService staffPermissionService;

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR','STAFF')")
    @Operation(summary = "Tao phieu nhap kho")
    public ResponseEntity<ApiResponse<InventoryImportResponse>> importInventory(
            Authentication authentication,
            @Valid @RequestBody InventoryImportRequest request
    ) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == Role.STAFF && !staffPermissionService.hasPermission(user.getId(), "INVENTORY:IMPORT")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        InventoryImportResponse result = inventoryService.importInventory(user.getId(), user.getRole(), request);

        return ResponseEntity.ok(ApiResponse.<InventoryImportResponse>builder()
                .code(1000)
                .message("Nhap kho thanh cong")
                .result(result)
                .build());
    }

    @PostMapping("/check/balance")
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR','STAFF')")
    @Operation(summary = "Can bang kho theo phieu kiem ke")
    public ResponseEntity<ApiResponse<InventoryCheckResponse>> balanceInventory(
            Authentication authentication,
            @Valid @RequestBody InventoryCheckBalanceRequest request
    ) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == Role.STAFF && !staffPermissionService.hasPermission(user.getId(), "INVENTORY:CHECK")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        InventoryCheckResponse result = inventoryService.balanceInventory(user.getId(), user.getRole(), request);

        return ResponseEntity.ok(ApiResponse.<InventoryCheckResponse>builder()
                .code(1000)
                .message("Can bang kho thanh cong")
                .result(result)
                .build());
    }

    @GetMapping("/imports")
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR','STAFF')")
    @Operation(summary = "Lay lich su nhap kho")
    public ResponseEntity<ApiResponse<List<InventoryImportHistoryResponse>>> getImportHistory(
            Authentication authentication,
            @RequestParam(required = false) String shopId,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == Role.STAFF && !staffPermissionService.hasPermission(user.getId(), "INVENTORY:VIEW")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        List<InventoryImportHistoryResponse> result = inventoryService.getImportHistory(
                user.getId(),
                user.getRole(),
                shopId,
                limit
        );

        return ResponseEntity.ok(ApiResponse.<List<InventoryImportHistoryResponse>>builder()
                .code(1000)
                .message("Lay lich su nhap kho thanh cong")
                .result(result)
                .build());
    }

    @GetMapping("/checks")
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR','STAFF')")
    @Operation(summary = "Lay lich su kiem ke kho")
    public ResponseEntity<ApiResponse<List<InventoryCheckHistoryResponse>>> getCheckHistory(
            Authentication authentication,
            @RequestParam(required = false) String shopId,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == Role.STAFF && !staffPermissionService.hasPermission(user.getId(), "INVENTORY:VIEW")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        List<InventoryCheckHistoryResponse> result = inventoryService.getCheckHistory(
                user.getId(),
                user.getRole(),
                shopId,
                limit
        );

        return ResponseEntity.ok(ApiResponse.<List<InventoryCheckHistoryResponse>>builder()
                .code(1000)
                .message("Lay lich su kiem ke kho thanh cong")
                .result(result)
                .build());
    }

    @GetMapping("/skus/search")
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR','STAFF')")
    @Operation(summary = "Tim SKU theo skuCode hoac ten san pham")
    public ResponseEntity<ApiResponse<List<ProductSkuSearchResponse>>> searchSkus(
            Authentication authentication,
            @RequestParam String keyword,
            @RequestParam(required = false) String shopId,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == Role.STAFF && !staffPermissionService.hasPermission(user.getId(), "INVENTORY:VIEW")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        List<ProductSkuSearchResponse> result = inventoryService.searchSkus(
                user.getId(),
                user.getRole(),
                shopId,
                keyword,
                limit
        );

        return ResponseEntity.ok(ApiResponse.<List<ProductSkuSearchResponse>>builder()
                .code(1000)
                .message("Tim SKU thanh cong")
                .result(result)
                .build());
    }
}
