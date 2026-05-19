package com.example.cellex.controllers;

import com.example.cellex.dtos.request.shop.ShopVerificationRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.shop.ShopResponse;
import com.example.cellex.enums.ShopStatus;
import com.example.cellex.models.user.User;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.services.shop.ShopService;
import com.example.cellex.services.staff.StaffPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import com.example.cellex.utils.PaginationUtil;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Tag(name = "08. Shop Management", description = "APIs for shop management")
@SecurityRequirement(name = "bearerAuth")
public class ShopController {

    private final ShopService shopService;
    private final StaffPermissionService staffPermissionService;

    // CREATE - Multipart Form Data
    @PostMapping(value = "/register-vendor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Đăng ký trở thành vendor",
        description = "User đăng ký tạo cửa hàng để trở thành vendor. Tên tỉnh/xã sẽ được tự động lấy từ hệ thống dựa trên mã."
    )
    public ResponseEntity<ApiResponse<ShopResponse>> registerVendor(
            @Parameter(description = "Tên cửa hàng", required = true, example = "Shop Công Nghệ ABC")
            @RequestPart("shopName") @NotBlank String shopName,

            @Parameter(description = "Mô tả cửa hàng", example = "Chuyên cung cấp điện thoại, laptop chính hãng")
            @RequestPart(value = "description", required = false) String description,

            @Parameter(description = "Mã tỉnh/thành phố", required = true, example = "01")
            @RequestPart("provinceCode") @NotBlank String provinceCode,

            @Parameter(description = "Mã xã/phường/thị trấn", required = true, example = "00001")
            @RequestPart("communeCode") @NotBlank String communeCode,

            @Parameter(description = "Địa chỉ chi tiết (số nhà, ngõ/hẻm, đường)", required = true, example = "Số 123, Ngõ 456, Đường Láng")
            @RequestPart("detailAddress") @NotBlank String detailAddress,

            @Parameter(description = "Số điện thoại cửa hàng", required = true, example = "0987654321")
            @RequestPart("phoneNumber") @NotBlank String phoneNumber,

            @Parameter(description = "Email cửa hàng", required = true, example = "shop@example.com")
            @RequestPart("email") @NotBlank @Email String email,

            @Parameter(description = "Logo cửa hàng (file ảnh)")
            @RequestPart(value = "logo", required = false) MultipartFile logoFile,

            Authentication authentication) throws IOException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((User) userDetails).getId();

        ShopResponse shopResponse = shopService.registerVendorShopMultipart(
                userId, shopName, description, provinceCode, communeCode,
                detailAddress, phoneNumber, email, logoFile);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Đăng ký cửa hàng thành công. Vui lòng chờ admin duyệt.")
                .result(shopResponse)
                .build());
    }

    // UPDATE - Multipart Form Data
    @PutMapping(value = "/my-shop", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    @Operation(
        summary = "Vendor cập nhật cửa hàng của mình",
        description = "Vendor cập nhật thông tin cửa hàng của mình. Shop ID được tự động lấy từ vendor ID trong JWT."
    )
    public ResponseEntity<ApiResponse<ShopResponse>> updateMyShop(
            @Parameter(description = "Tên cửa hàng", example = "Shop Công Nghệ ABC")
            @RequestPart(value = "shopName", required = false) String shopName,

            @Parameter(description = "Mô tả cửa hàng", example = "Chuyên cung cấp điện thoại, laptop chính hãng")
            @RequestPart(value = "description", required = false) String description,

            @Parameter(description = "Mã tỉnh/thành phố", example = "01")
            @RequestPart(value = "provinceCode", required = false) String provinceCode,

            @Parameter(description = "Mã xã/phường/thị trấn", example = "00001")
            @RequestPart(value = "communeCode", required = false) String communeCode,

            @Parameter(description = "Địa chỉ chi tiết (số nhà, ngõ/hẻm, đường)", example = "Số 123, Ngõ 456, Đường Láng")
            @RequestPart(value = "detailAddress", required = false) String detailAddress,

            @Parameter(description = "Số điện thoại cửa hàng", example = "0987654321")
            @RequestPart(value = "phoneNumber", required = false) String phoneNumber,

            @Parameter(description = "Email cửa hàng", example = "shop@example.com")
            @RequestPart(value = "email", required = false) String email,

            @Parameter(description = "Logo cửa hàng mới (file ảnh)")
            @RequestPart(value = "logo", required = false) MultipartFile logoFile,

            Authentication authentication) throws IOException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User operator = (User) userDetails;
        if (operator.getRole() == Role.STAFF && !staffPermissionService.hasPermission(operator.getId(), "SHOP:UPDATE")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        String vendorId = operator.getId();

        ShopResponse shopResponse = shopService.updateMyShop(
                vendorId, shopName, description, provinceCode, communeCode,
                detailAddress, phoneNumber, email, logoFile);

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message("Cập nhật thông tin cửa hàng thành công.")
                .result(shopResponse)
                .build());
    }

    @PutMapping(value = "/{shopId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Admin cập nhật bất kỳ cửa hàng nào",
        description = "Admin có thể cập nhật thông tin của bất kỳ cửa hàng nào dựa trên shop ID."
    )
    public ResponseEntity<ApiResponse<ShopResponse>> updateShopByAdmin(
            @PathVariable String shopId,

            @Parameter(description = "Tên cửa hàng", example = "Shop Công Nghệ ABC")
            @RequestPart(value = "shopName", required = false) String shopName,

            @Parameter(description = "Mô tả cửa hàng", example = "Chuyên cung cấp điện thoại, laptop chính hãng")
            @RequestPart(value = "description", required = false) String description,

            @Parameter(description = "Mã tỉnh/thành phố", example = "01")
            @RequestPart(value = "provinceCode", required = false) String provinceCode,

            @Parameter(description = "Mã xã/phường/thị trấn", example = "00001")
            @RequestPart(value = "communeCode", required = false) String communeCode,

            @Parameter(description = "Địa chỉ chi tiết (số nhà, ngõ/hẻm, đường)", example = "Số 123, Ngõ 456, Đường Láng")
            @RequestPart(value = "detailAddress", required = false) String detailAddress,

            @Parameter(description = "Số điện thoại cửa hàng", example = "0987654321")
            @RequestPart(value = "phoneNumber", required = false) String phoneNumber,

            @Parameter(description = "Email cửa hàng", example = "shop@example.com")
            @RequestPart(value = "email", required = false) String email,

            @Parameter(description = "Logo cửa hàng mới (file ảnh)")
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) throws IOException {

        ShopResponse shopResponse = shopService.updateShopByAdmin(
                shopId, shopName, description, provinceCode, communeCode,
                detailAddress, phoneNumber, email, logoFile);

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

        String message = "APPROVED".equals(request.getStatus())
            ? "Duyệt cửa hàng thành công"
            : "Từ chối cửa hàng thành công";

        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .code(200)
                .message(message)
                .result(shopResponse)
                .build());
    }

    @GetMapping("/my-shop")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    @Operation(summary = "Lấy thông tin cửa hàng của vendor", description = "Vendor xem thông tin cửa hàng của mình")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop(Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = ((User) userDetails).getId();

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
            description = "Lấy danh sách tất cả cửa hàng với phân trang, có thể lọc theo trạng thái (PENDING, APPROVED, REJECTED)"
    )
    public ResponseEntity<ApiResponse<PageResponse<ShopResponse>>> getAllShops(
            @Parameter(description = "Lọc theo trạng thái (PENDING/APPROVED/REJECTED). Không truyền để lấy tất cả.")
            @RequestParam(required = false) ShopStatus status,

            @Parameter(description = "Số trang (bắt đầu từ 1)")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "Số lượng cửa hàng mỗi trang")
            @RequestParam(defaultValue = "10") Integer limit,

            @Parameter(description = "Trường sắp xếp")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Kiểu sắp xếp (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType) {

        Pageable pageable = PaginationUtil.createPageable(page, limit, sortBy, sortType);
        PageResponse<ShopResponse> shops = shopService.getAllShops(status, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ShopResponse>>builder()
                .code(200)
                .message("Lấy danh sách cửa hàng thành công")
                .result(shops)
                .build());
    }
}
