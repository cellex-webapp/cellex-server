package com.example.cellex.controllers;

import com.example.cellex.dtos.request.ShopVerificationRequest;
import com.example.cellex.dtos.request.VendorRegistrationRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.ShopResponse;
import com.example.cellex.services.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Tag(name = "Shop Management", description = "APIs for shop management")
@SecurityRequirement(name = "bearerAuth")
public class ShopController {

    private final ShopService shopService;

    @PostMapping(value = "/register-vendor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Đăng ký trở thành vendor", description = "User đăng ký tạo cửa hàng để trở thành vendor với upload logo")
    public ResponseEntity<ApiResponse<ShopResponse>> registerVendor(
            @RequestPart("request") @Valid VendorRegistrationRequest request,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile,
            Authentication authentication) throws IOException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((com.example.cellex.models.User) userDetails).getId();

        ShopResponse shopResponse = shopService.registerVendorShop(userId, request, logoFile);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Đăng ký cửa hàng thành công. Vui lòng chờ admin duyệt.")
                .result(shopResponse)
                .build());
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Duyệt cửa hàng", description = "Admin duyệt hoặc từ chối cửa hàng")
    public ResponseEntity<ApiResponse<ShopResponse>> verifyShop(
            @Valid @RequestBody ShopVerificationRequest request) {

        ShopResponse shopResponse = shopService.verifyShop(request);

        String message = "APPROVE".equals(request.getStatus())
            ? "Duyệt cửa hàng thành công"
            : "Từ chối cửa hàng thành công";

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message(message)
                .result(shopResponse)
                .build());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách cửa hàng chờ duyệt", description = "Admin xem danh sách các cửa hàng chờ duyệt")
    public ResponseEntity<ApiResponse<List<ShopResponse>>> getPendingShops() {

        List<ShopResponse> pendingShops = shopService.getPendingShops();

        return ResponseEntity.ok(ApiResponse.<List<ShopResponse>>builder()
                .code(200)
                .message("Lấy danh sách cửa hàng chờ duyệt thành công")
                .result(pendingShops)
                .build());
    }

    @GetMapping("/my-shop")
    @Operation(summary = "Lấy thông tin cửa hàng của vendor", description = "Vendor xem thông tin cửa hàng của mình")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop(Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((com.example.cellex.models.User) userDetails).getId();

        ShopResponse shopResponse = shopService.getShopByVendorId(userId);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Lấy thông tin cửa hàng thành công")
                .result(shopResponse)
                .build());
    }

    @GetMapping("/{shopId}")
    @Operation(summary = "Lấy thông tin cửa hàng theo ID", description = "Lấy thông tin chi tiết cửa hàng")
    public ResponseEntity<ApiResponse<ShopResponse>> getShopById(@PathVariable String shopId) {

        ShopResponse shopResponse = shopService.getShopById(shopId);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Lấy thông tin cửa hàng thành công")
                .result(shopResponse)
                .build());
    }
}
