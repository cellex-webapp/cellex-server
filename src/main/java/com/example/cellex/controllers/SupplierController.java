package com.example.cellex.controllers;

import com.example.cellex.dtos.request.inventory.CreateSupplierRequest;
import com.example.cellex.dtos.request.inventory.UpdateSupplierRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.inventory.SupplierResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.services.inventory.SupplierService;
import com.example.cellex.services.staff.StaffPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "14. Suppliers", description = "APIs quan ly nha cung cap")
public class SupplierController {

    private final SupplierService supplierService;
    private final StaffPermissionService staffPermissionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR','STAFF')")
    @Operation(summary = "Lay danh sach nha cung cap", description = "Ho tro phan trang va tim kiem theo ten")
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponse>>> getSuppliers(
            Authentication authentication,
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortType
    ) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == Role.STAFF && !staffPermissionService.hasPermission(user.getId(), "SUPPLIER:VIEW")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));

        PageResponse<SupplierResponse> result = supplierService.getSuppliers(
                user.getId(),
                user.getRole(),
                shopId,
                search,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.<PageResponse<SupplierResponse>>builder()
                .code(1000)
                .message("Lay danh sach nha cung cap thanh cong")
                .result(result)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR','STAFF')")
    @Operation(summary = "Tao nha cung cap")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            Authentication authentication,
            @Valid @RequestBody CreateSupplierRequest request
    ) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == Role.STAFF && !staffPermissionService.hasPermission(user.getId(), "SUPPLIER:CREATE")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }

        SupplierResponse result = supplierService.createSupplier(user.getId(), user.getRole(), request);

        return ResponseEntity.ok(ApiResponse.<SupplierResponse>builder()
                .code(1000)
                .message("Tao nha cung cap thanh cong")
                .result(result)
                .build());
    }

    @PutMapping("/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR','STAFF')")
    @Operation(summary = "Cap nhat nha cung cap")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            Authentication authentication,
            @PathVariable String supplierId,
            @Valid @RequestBody UpdateSupplierRequest request
    ) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == Role.STAFF && !staffPermissionService.hasPermission(user.getId(), "SUPPLIER:UPDATE")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }

        SupplierResponse result = supplierService.updateSupplier(user.getId(), user.getRole(), supplierId, request);

        return ResponseEntity.ok(ApiResponse.<SupplierResponse>builder()
                .code(1000)
                .message("Cap nhat nha cung cap thanh cong")
                .result(result)
                .build());
    }
}
