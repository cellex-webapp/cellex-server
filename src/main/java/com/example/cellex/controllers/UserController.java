package com.example.cellex.controllers;

import com.example.cellex.dtos.request.profile.CreateUserDataRequest;
import com.example.cellex.dtos.request.profile.UpdateUserRequest;
import com.example.cellex.dtos.request.profile.UpdateUserDataRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.UserResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.models.User;
import com.example.cellex.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

@Tag(name = "User Management", description = "APIs for managing user accounts and profiles")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

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
    public ApiResponse<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ApiResponse.<List<UserResponse>>builder()
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

    // CREATE - JSON Data
    @Operation(
            summary = "Create a new user account",
            description = "Creates a new user account with role and address information using JSON data."
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
    @PostMapping("/add-account")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> addAccount(@Valid @RequestBody CreateUserDataRequest userData) throws IOException {
        User createdUser = userService.createAccount(userData, null);
        return ApiResponse.<User>builder()
                .result(createdUser)
                .message("Account created successfully.")
                .build();
    }

    // CREATE - Upload Avatar
    @Operation(
            summary = "Upload avatar for user",
            description = "Uploads an avatar for an existing user account."
    )
    @PostMapping("/{userId}/upload-avatar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> uploadUserAvatar(
            @PathVariable String userId,
            @Parameter(description = "Avatar image file", required = true)
            @RequestParam("avatar") MultipartFile avatar) throws IOException {

        UserResponse updatedUser = userService.uploadUserAvatar(userId, avatar);
        return ApiResponse.<UserResponse>builder()
                .result(updatedUser)
                .message("User avatar uploaded successfully.")
                .build();
    }

    // UPDATE - JSON Data
    @Operation(
            summary = "Update user profile",
            description = "Updates the current user's profile information using JSON data."
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
    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserDataRequest request) throws IOException {

        User currentUser = (User) authentication.getPrincipal();
        UserResponse updatedUser = userService.updateProfile(currentUser.getId(), request);

        return ApiResponse.<UserResponse>builder()
                .result(updatedUser)
                .message("Profile updated successfully.")
                .build();
    }

    // UPDATE - Upload Avatar
    @Operation(
            summary = "Update user avatar",
            description = "Updates the current user's avatar image."
    )
    @PutMapping("/me/upload-avatar")
    public ApiResponse<UserResponse> updateUserAvatar(
            Authentication authentication,
            @Parameter(description = "Avatar image file", required = true)
            @RequestParam("avatar") MultipartFile avatar) throws IOException {

        User currentUser = (User) authentication.getPrincipal();
        UserResponse updatedUser = userService.uploadUserAvatar(currentUser.getId(), avatar);

        return ApiResponse.<UserResponse>builder()
                .result(updatedUser)
                .message("Avatar updated successfully.")
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
            @Valid @RequestBody com.example.cellex.dtos.request.BanUserRequest request,
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
}
