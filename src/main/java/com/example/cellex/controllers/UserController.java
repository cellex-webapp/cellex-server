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

    @Operation(
            summary = "Create a new user account",
            description = "Creates a new user account with role, avatar, and address information."
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
                    description = "Invalid request data or file upload failed",
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
            @Valid @Parameter(
                    description = "User data in JSON format", required = true,
                    examples = @ExampleObject(value = """
                        {
                          "fullName": "Nguyễn Văn An",
                          "email": "admin@example.com",
                          "password": "Password123",
                          "phoneNumber": "0987654321",
                          "role": "ADMIN",
                          "provinceCode": "01",
                          "communeCode": "00001",
                          "detailAddress": "123 Đường Lê Lợi"
                        }
                        """),
                    content = @Content(schema = @Schema(implementation = CreateUserDataRequest.class))
            )
            @RequestPart("data") CreateUserDataRequest userData,

            @Parameter(
                    description = "Avatar image file (optional)",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) throws IOException {

        User createdUser = userService.createAccount(userData, avatar);
        return ApiResponse.<User>builder()
                .result(createdUser)
                .message("Account created successfully.")
                .build();
    }

    @Operation(
            summary = "Update user profile",
            description = "Updates the current user's profile including full name, avatar, and address."
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
                    description = "Invalid input data or file upload failed",
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

            @Valid @Parameter(
                    description = "User data in JSON format", required = true,
                    examples = @ExampleObject(value = """
                        {
                          "fullName": "Nguyễn Văn An",
                          "phoneNumber": "0987654321",
                          "provinceCode": "01",
                          "communeCode": "00001",
                          "detailAddress": "123 Đường Lê Lợi"
                        }
                        """),
                    content = @Content(schema = @Schema(implementation = UpdateUserDataRequest.class))
            )
            @RequestPart("data") UpdateUserDataRequest userData,

            @Parameter(
                    description = "Avatar image file (optional)",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) throws IOException {

        User currentUser = (User) authentication.getPrincipal();

        // Build UpdateUserRequest from userData and avatar
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName(userData.getFullName())
                .phoneNumber(userData.getPhoneNumber())
                .avatar(avatar)
                .provinceCode(userData.getProvinceCode())
                .communeCode(userData.getCommuneCode())
                .detailAddress(userData.getDetailAddress())
                .build();

        UserResponse updatedUser = userService.updateProfile(currentUser.getId(), request);

        return ApiResponse.<UserResponse>builder()
                .result(updatedUser)
                .message("Profile updated successfully.")
                .build();
    }
}
