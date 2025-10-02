package com.example.cellex.controllers;

import com.example.cellex.dtos.request.profile.CreateUserRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.models.User;
import com.example.cellex.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "User Management", description = "APIs for managing user accounts")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // CREATE USER
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
}
