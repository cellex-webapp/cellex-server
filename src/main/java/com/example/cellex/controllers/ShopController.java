package com.example.cellex.controllers;

import com.example.cellex.dtos.request.ShopVerificationRequest;
import com.example.cellex.dtos.request.VendorRegistrationRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.ShopResponse;
import com.example.cellex.services.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    // CREATE - JSON Data
    @PostMapping("/register-vendor")
    @Operation(summary = "Đăng ký trở thành vendor", description = "User đăng ký tạo cửa hàng để trở thành vendor sử dụng JSON data")
    public ResponseEntity<ApiResponse<ShopResponse>> registerVendor(
            @Valid @RequestBody VendorRegistrationRequest request,
            Authentication authentication) throws IOException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((com.example.cellex.models.User) userDetails).getId();

        ShopResponse shopResponse = shopService.registerVendorShop(userId, request, null);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Đăng ký cửa hàng thành công. Vui lòng chờ admin duyệt.")
                .result(shopResponse)
                .build());
    }

    // CREATE - Upload Logo
    @PostMapping("/{shopId}/upload-logo")
    @Operation(summary = "Upload logo cho cửa hàng", description = "Upload logo cho cửa hàng đã tạo")
    public ResponseEntity<ApiResponse<ShopResponse>> uploadShopLogo(
            @PathVariable String shopId,
            @Parameter(description = "Logo cửa hàng", required = true)
            @RequestParam("logo") MultipartFile logoFile,
            Authentication authentication) throws IOException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((com.example.cellex.models.User) userDetails).getId();

        ShopResponse shopResponse = shopService.uploadShopLogo(shopId, userId, logoFile);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Upload logo cửa hàng thành công.")
                .result(shopResponse)
                .build());
    }

    // UPDATE - JSON Data
    @PutMapping("/{shopId}")
    @Operation(summary = "Cập nhật thông tin cửa hàng", description = "Vendor cập nhật thông tin cửa hàng sử dụng JSON data")
    public ResponseEntity<ApiResponse<ShopResponse>> updateShop(
            @PathVariable String shopId,
            @Valid @RequestBody VendorRegistrationRequest request,
            Authentication authentication) throws IOException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((com.example.cellex.models.User) userDetails).getId();

        ShopResponse shopResponse = shopService.updateShop(shopId, userId, request);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Cập nhật thông tin cửa hàng thành công.")
                .result(shopResponse)
                .build());
    }

    // UPDATE - Upload Logo
    @PutMapping("/{shopId}/upload-logo")
    @Operation(summary = "Cập nhật logo cửa hàng", description = "Vendor cập nhật logo cửa hàng")
    public ResponseEntity<ApiResponse<ShopResponse>> updateShopLogo(
            @PathVariable String shopId,
            @Parameter(description = "Logo cửa hàng mới", required = true)
            @RequestParam("logo") MultipartFile logoFile,
            Authentication authentication) throws IOException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((com.example.cellex.models.User) userDetails).getId();

        ShopResponse shopResponse = shopService.uploadShopLogo(shopId, userId, logoFile);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Cập nhật logo cửa hàng thành công.")
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
