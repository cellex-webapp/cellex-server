package com.example.cellex.controllers;

import com.example.cellex.dtos.request.address.CreateUserAddressRequest;
import com.example.cellex.dtos.request.address.UpdateUserAddressRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.address.UserAddressResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.services.address.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "04. User Address Book", description = "APIs for managing user delivery addresses (UC-01)")
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserAddressController {

    private final UserAddressService userAddressService;

    @Operation(
            summary = "Lấy danh sách địa chỉ",
            description = "Lấy toàn bộ danh sách địa chỉ giao hàng của người dùng đang đăng nhập. Địa chỉ mặc định sẽ hiển thị đầu tiên."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Danh sách địa chỉ lấy thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Chưa xác thực - JWT không hợp lệ",
                    content = @Content
            )
    })
    @GetMapping
    public ApiResponse<List<UserAddressResponse>> getAddresses(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<UserAddressResponse> addresses = userAddressService.getUserAddresses(currentUser.getId());
        return ApiResponse.<List<UserAddressResponse>>builder()
                .result(addresses)
                .message("Lấy danh sách địa chỉ thành công.")
                .build();
    }

    @Operation(
            summary = "Lấy chi tiết địa chỉ",
            description = "Lấy thông tin chi tiết một địa chỉ theo ID."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Lấy địa chỉ thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Không tìm thấy địa chỉ",
                    content = @Content
            )
    })
    @GetMapping("/{addressId}")
    public ApiResponse<UserAddressResponse> getAddressById(
            Authentication authentication,
            @Parameter(description = "Address ID") @PathVariable String addressId) {
        User currentUser = (User) authentication.getPrincipal();
        UserAddressResponse address = userAddressService.getAddressById(currentUser.getId(), addressId);
        return ApiResponse.<UserAddressResponse>builder()
                .result(address)
                .message("Lấy địa chỉ thành công.")
                .build();
    }

    @Operation(
            summary = "Thêm địa chỉ mới",
            description = "Tạo một địa chỉ giao hàng mới. Nếu là địa chỉ đầu tiên, tự động đặt làm mặc định. " +
                    "Trường tag (Nhà riêng, Công ty...) là không bắt buộc."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Tạo địa chỉ thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Dữ liệu không hợp lệ hoặc đã đạt giới hạn địa chỉ",
                    content = @Content
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserAddressResponse> createAddress(
            Authentication authentication,
            @Valid @RequestBody CreateUserAddressRequest request) {
        User currentUser = (User) authentication.getPrincipal();
        UserAddressResponse address = userAddressService.createAddress(currentUser.getId(), request);
        return ApiResponse.<UserAddressResponse>builder()
                .code(201)
                .result(address)
                .message("Thêm địa chỉ thành công.")
                .build();
    }

    @Operation(
            summary = "Cập nhật địa chỉ",
            description = "Chỉnh sửa thông tin một địa chỉ hiện có. Khi đặt isDefault = true, " +
                    "tất cả địa chỉ khác sẽ tự động chuyển về không mặc định."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cập nhật địa chỉ thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Không tìm thấy địa chỉ",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Dữ liệu không hợp lệ",
                    content = @Content
            )
    })
    @PutMapping("/{addressId}")
    public ApiResponse<UserAddressResponse> updateAddress(
            Authentication authentication,
            @Parameter(description = "Address ID") @PathVariable String addressId,
            @Valid @RequestBody UpdateUserAddressRequest request) {
        User currentUser = (User) authentication.getPrincipal();
        UserAddressResponse address = userAddressService.updateAddress(currentUser.getId(), addressId, request);
        return ApiResponse.<UserAddressResponse>builder()
                .result(address)
                .message("Cập nhật địa chỉ thành công.")
                .build();
    }

    @Operation(
            summary = "Xóa địa chỉ",
            description = "Xóa một địa chỉ giao hàng. Nếu xóa địa chỉ mặc định, " +
                    "địa chỉ mới nhất còn lại sẽ được tự động đặt làm mặc định."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Xóa địa chỉ thành công"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Không tìm thấy địa chỉ",
                    content = @Content
            )
    })
    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> deleteAddress(
            Authentication authentication,
            @Parameter(description = "Address ID") @PathVariable String addressId) {
        User currentUser = (User) authentication.getPrincipal();
        userAddressService.deleteAddress(currentUser.getId(), addressId);
        return ApiResponse.<Void>builder()
                .message("Xóa địa chỉ thành công.")
                .build();
    }
}
