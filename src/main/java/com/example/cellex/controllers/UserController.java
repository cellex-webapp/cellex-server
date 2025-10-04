package com.example.cellex.controllers;

import com.example.cellex.dtos.request.profile.CreateUserRequest;
import com.example.cellex.dtos.request.profile.UpdateUserRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.UserResponse;
import com.example.cellex.models.User;
import com.example.cellex.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

@Tag(name = "User Management", description = "APIs for managing user accounts")
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
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)) }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Admin role required",
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
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)) }
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
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)) }
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
                .message("Current user profile retrieved successfully.")
                .build();
    }

    @Operation(
            summary = "Create a new user account",
            description = "Creates a new user with the ADMIN role and an active status by default. The email must be unique."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Account created successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)) }
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
            )
    })
    @PostMapping("/add-account")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> addAccount(@Valid @RequestBody CreateUserRequest request) {
        User createdUser = userService.createAccount(request);
        return ApiResponse.<User>builder()
                .result(createdUser)
                .message("Account created successfully.")
                .build();
    }

    @Operation(
            summary = "Update user profile",
            description = "Updates user profile including full name, avatar, and address. All fields are optional."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)) }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            )
    })
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @ModelAttribute UpdateUserRequest request) throws IOException {

        User currentUser = (User) authentication.getPrincipal();
        UserResponse updatedUser = userService.updateProfile(currentUser.getId(), request);

        return ApiResponse.<UserResponse>builder()
                .result(updatedUser)
                .message("Profile updated successfully.")
                .build();
    }
}
