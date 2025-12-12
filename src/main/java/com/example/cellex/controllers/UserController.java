package com.example.cellex.controllers;

import com.example.cellex.dtos.request.auth.ChangePasswordRequest;
import com.example.cellex.dtos.request.user.BanUserRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.user.UserResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.services.auth.AuthService;
import com.example.cellex.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.List;

@Tag(name = "02. User Management", description = "APIs for managing user accounts and profiles")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @Operation(
            summary = "Get all users",
            description = "Retrieves a list of all users in the system. Only accessible by ADMIN role."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Admin role required",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content
            )
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.example.cellex.dtos.response.PageResponse<UserResponse>> getAllUsers(
            @Parameter(description = "Số trang (bắt đầu từ 1)")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "Số lượng user mỗi trang")
            @RequestParam(defaultValue = "10") Integer limit,

            @Parameter(description = "Sắp xếp theo (createdAt, fullName, email)")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Kiểu sắp xếp (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType) {

        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));
        com.example.cellex.dtos.response.PageResponse<UserResponse> users = userService.getAllUsers(pageable);

        return ApiResponse.<com.example.cellex.dtos.response.PageResponse<UserResponse>>builder()
                .result(users)
                .message("Users retrieved successfully.")
                .build();
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a specific user by their ID. Only accessible by ADMIN role."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Admin role required",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content
            )
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getUserById(
            @Parameter(description = "User ID", example = "60c72b2f9b1d8c001f8e4c8c")
            @PathVariable String userId) {
        UserResponse user = userService.getUserById(userId);
        return ApiResponse.<UserResponse>builder()
                .result(user)
                .message("User retrieved successfully.")
                .build();
    }

    @Operation(
            summary = "Get current user profile",
            description = "Retrieves the profile information of the currently authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Current user profile retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Valid JWT token required",
                    content = @Content
            )
    })
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        UserResponse user = userService.getCurrentUser(currentUser.getId());
        return ApiResponse.<UserResponse>builder()
                .result(user)
                .message("User retrieved successfully.")
                .build();
    }

    // CREATE - Multipart Form Data
    @Operation(
            summary = "Create a new user account",
            description = "Creates a new user account with role and address information using multipart form data with optional avatar upload."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Account created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email already in use",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Admin role required",
                    content = @Content
            )
    })
    @PostMapping(value = "/add-account", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> addAccount(
            @Parameter(description = "Họ và tên", required = true)
            @RequestPart("fullName") @NotBlank String fullName,

            @Parameter(description = "Email", required = true)
            @RequestPart("email") @NotBlank @Email String email,

            @Parameter(description = "Mật khẩu", required = true)
            @RequestPart("password") @NotBlank String password,

            @Parameter(description = "Số điện thoại")
            @RequestPart(value = "phoneNumber", required = false) String phoneNumber,

            @Parameter(description = "Vai trò", required = true)
            @RequestPart("role") @NotBlank String role,

            @Parameter(description = "Mã tỉnh/thành phố")
            @RequestPart(value = "provinceCode", required = false) String provinceCode,

            @Parameter(description = "Mã xã/phường")
            @RequestPart(value = "communeCode", required = false) String communeCode,

            @Parameter(description = "Địa chỉ chi tiết")
            @RequestPart(value = "detailAddress", required = false) String detailAddress,

            @Parameter(description = "Ảnh đại diện")
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) throws IOException {

        User createdUser = userService.createAccountMultipart(
                fullName, email, password, phoneNumber, role,
                provinceCode, communeCode, detailAddress, avatar);
        return ApiResponse.<User>builder()
                .result(createdUser)
                .message("Account created successfully.")
                .build();
    }

    // UPDATE - Multipart Form Data
    @Operation(
            summary = "Update user profile",
            description = "Updates the current user's profile information using multipart form data with optional avatar upload."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Valid JWT token required",
                    content = @Content
            )
    })
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateProfile(
            Authentication authentication,

            @Parameter(description = "Họ và tên")
            @RequestPart(value = "fullName", required = false) String fullName,

            @Parameter(description = "Số điện thoại")
            @RequestPart(value = "phoneNumber", required = false) String phoneNumber,

            @Parameter(description = "Mã tỉnh/thành phố")
            @RequestPart(value = "provinceCode", required = false) String provinceCode,

            @Parameter(description = "Mã xã/phường")
            @RequestPart(value = "communeCode", required = false) String communeCode,

            @Parameter(description = "Địa chỉ chi tiết")
            @RequestPart(value = "detailAddress", required = false) String detailAddress,

            @Parameter(description = "Ảnh đại diện mới")
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) throws IOException {

        User currentUser = (User) authentication.getPrincipal();
        UserResponse updatedUser = userService.updateProfileMultipart(
                currentUser.getId(), fullName, phoneNumber,
                provinceCode, communeCode, detailAddress, avatar);

        return ApiResponse.<UserResponse>builder()
                .result(updatedUser)
                .message("Profile updated successfully.")
                .build();
    }

    // ADMIN - Lock User Account
    @PostMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Khóa tài khoản người dùng",
            description = "Admin khóa tài khoản người dùng với lý do cụ thể. Người dùng bị khóa sẽ không thể đăng nhập.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<UserResponse> lockUser(
            @PathVariable String userId,
            @Valid @RequestBody BanUserRequest request,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();
        UserResponse lockedUser = userService.banUser(userId, request.getBanReason(), admin.getId());

        return ApiResponse.<UserResponse>builder()
                .result(lockedUser)
                .message("Tài khoản đã được khóa thành công.")
                .build();
    }

    // ADMIN - Unlock User Account
    @PostMapping("/{userId}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Mở khóa tài khoản người dùng",
            description = "Admin mở khóa tài khoản người dùng đã bị khóa trước đó.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<UserResponse> unlockUser(
            @PathVariable String userId,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();
        UserResponse unlockedUser = userService.unbanUser(userId, admin.getId());

        return ApiResponse.<UserResponse>builder()
                .result(unlockedUser)
                .message("Tài khoản đã được mở khóa thành công.")
                .build();
    }

    @PostMapping("/change-password")
    @Operation(
            summary = "Đổi mật khẩu",
            description = "Đổi mật khẩu cho tài khoản hiện tại (tất cả các role). Yêu cầu mật khẩu cũ và mật khẩu mới."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Đổi mật khẩu thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Không có quyền truy cập hoặc mật khẩu cũ không đúng",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Định dạng mật khẩu không hợp lệ",
                    content = @Content
            )
    })
    public ApiResponse<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        authService.changePassword(user.getEmail(), request);
        return ApiResponse.<String>builder()
                .message("Đổi mật khẩu thành công.")
                .build();
    }
}
