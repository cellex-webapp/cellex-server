package com.example.cellex.controllers;

import com.example.cellex.dtos.request.ShopVerificationRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.ShopResponse;
import com.example.cellex.enums.ShopStatus;
import com.example.cellex.services.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "08. Shop Management", description = "APIs for shop management")
@SecurityRequirement(name = "bearerAuth")
public class ShopController {

    private final ShopService shopService;

    // CREATE - Multipart Form Data
    @PostMapping(value = "/register-vendor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Đăng ký trở thành vendor",
        description = "User đăng ký tạo cửa hàng để trở thành vendor sử dụng multipart form data với optional logo upload"
    )
    public ResponseEntity<ApiResponse<ShopResponse>> registerVendor(
            @Parameter(description = "Tên cửa hàng", required = true)
            @RequestPart("shopName") @NotBlank String shopName,

            @Parameter(description = "Mô tả cửa hàng")
            @RequestPart(value = "description", required = false) String description,

            @Parameter(description = "Địa chỉ cửa hàng", required = true)
            @RequestPart("address") @NotBlank String address,

            @Parameter(description = "Số điện thoại cửa hàng", required = true)
            @RequestPart("phoneNumber") @NotBlank String phoneNumber,

            @Parameter(description = "Email cửa hàng", required = true)
            @RequestPart("email") @NotBlank @Email String email,

            @Parameter(description = "Logo cửa hàng")
            @RequestPart(value = "logo", required = false) MultipartFile logoFile,

            Authentication authentication) throws IOException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((com.example.cellex.models.User) userDetails).getId();

        ShopResponse shopResponse = shopService.registerVendorShopMultipart(
                userId, shopName, description, address, phoneNumber, email, logoFile);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Đăng ký cửa hàng thành công. Vui lòng chờ admin duyệt.")
                .result(shopResponse)
                .build());
    }

    // UPDATE - Multipart Form Data
    @PutMapping(value = "/{shopId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Cập nhật thông tin cửa hàng",
        description = "Vendor cập nhật thông tin cửa hàng sử dụng multipart form data với optional logo upload"
    )
    public ResponseEntity<ApiResponse<ShopResponse>> updateShop(
            @PathVariable String shopId,

            @Parameter(description = "Tên cửa hàng")
            @RequestPart(value = "shopName", required = false) String shopName,

            @Parameter(description = "Mô tả cửa hàng")
            @RequestPart(value = "description", required = false) String description,

            @Parameter(description = "Địa chỉ cửa hàng")
            @RequestPart(value = "address", required = false) String address,

            @Parameter(description = "Số điện thoại cửa hàng")
            @RequestPart(value = "phoneNumber", required = false) String phoneNumber,

            @Parameter(description = "Email cửa hàng")
            @RequestPart(value = "email", required = false) String email,

            @Parameter(description = "Logo cửa hàng mới")
            @RequestPart(value = "logo", required = false) MultipartFile logoFile,

            Authentication authentication) throws IOException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((com.example.cellex.models.User) userDetails).getId();

        ShopResponse shopResponse = shopService.updateShopMultipart(
                shopId, userId, shopName, description, address, phoneNumber, email, logoFile);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Cập nhật thông tin cửa hàng thành công.")
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

    @GetMapping
    @Operation(
            summary = "Lấy tất cả cửa hàng",
            description = "Lấy danh sách tất cả cửa hàng, có thể lọc theo trạng thái (PENDING, APPROVED, REJECTED)"
    )
    public ResponseEntity<ApiResponse<List<ShopResponse>>> getAllShops(
            @Parameter(description = "Lọc theo trạng thái (PENDING/APPROVED/REJECTED). Không truyền để lấy tất cả.")
            @RequestParam(required = false) ShopStatus status) {

        List<ShopResponse> shops = shopService.getAllShops(status);

        return ResponseEntity.ok(ApiResponse.<List<ShopResponse>>builder()
                .code(200)
                .message("Lấy danh sách cửa hàng thành công")
                .result(shops)
                .build());
    }
}
