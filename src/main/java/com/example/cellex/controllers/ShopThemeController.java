package com.example.cellex.controllers;

import com.example.cellex.dtos.request.shop.ShopThemeRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.shop.ShopThemeResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.user.User;
import com.example.cellex.services.shop.ShopThemeService;
import com.example.cellex.services.staff.StaffPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Server-Driven UI (SDUI) - ShopTheme Management.
 * Handles theme configuration for Shop UI.
 *
 * Endpoints:
 * - GET  /api/v1/shops/{shopId}/theme         - Get theme
 * - POST /api/v1/shops/{shopId}/theme         - Create theme
 * - PUT  /api/v1/shops/{shopId}/theme         - Update theme
 * - DELETE /api/v1/shops/{shopId}/theme       - Delete theme
 */
@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "08. Shop Theme Management (SDUI)",
    description = "APIs for managing Server-Driven UI (SDUI) theme configuration for shops"
)
@SecurityRequirement(name = "bearerAuth")
public class ShopThemeController {

    private final ShopThemeService shopThemeService;
    private final StaffPermissionService staffPermissionService;

    /**
     * GET: Retrieve theme configuration for a shop.
     *
     * @param shopId the shop UUID
     * @return theme response DTO
     */
    @GetMapping("/{shopId}/theme")
    @Operation(
        summary = "Get shop theme configuration",
        description = "Retrieve the SDUI theme configuration for a specific shop. " +
                     "This includes color scheme, typography, and layout configuration."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Theme retrieved successfully",
            content = @Content(schema = @Schema(implementation = ShopThemeResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Theme not found for the shop"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid shop ID format"
        )
    })
    public ResponseEntity<ApiResponse<ShopThemeResponse>> getTheme(
            @PathVariable
            @Parameter(description = "Shop UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            UUID shopId) {

        log.info("GET /api/v1/shops/{}/theme", shopId);
        ShopThemeResponse response = shopThemeService.getThemeByShopId(shopId);

        return ResponseEntity.ok(ApiResponse.<ShopThemeResponse>builder()
            .code(200)
            .message("Get theme successfully")
            .result(response)
            .build());
    }

    /**
     * POST: Create a new theme configuration for a shop.
     * Only one theme per shop is allowed (unique shop_id constraint).
     * Only the shop owner can create a theme (BOLA protection).
     *
     * @param shopId the shop UUID
     * @param request the theme request DTO
     * @return created theme response DTO
     */
    @PostMapping("/{shopId}/theme")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create shop theme configuration",
        description = "Create a new SDUI theme configuration for a shop. " +
                     "Each shop can have only one theme. If a theme already exists, use PUT to update. " +
                     "Only the shop owner can create a theme."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Theme created successfully",
            content = @Content(schema = @Schema(implementation = ShopThemeResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - theme already exists or invalid data"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - user not authorized or not the shop owner"
        )
    })
    public ResponseEntity<ApiResponse<ShopThemeResponse>> createTheme(
            Authentication authentication,
            @PathVariable
            @Parameter(description = "Shop UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            UUID shopId,

            @RequestBody
            @Valid
            @Parameter(description = "Theme configuration", required = true)
            ShopThemeRequest request) {
        validateThemeManagementPermission(authentication, shopId);

        log.info("POST /api/v1/shops/{}/theme - Creating new theme", shopId);
        ShopThemeResponse response = shopThemeService.createTheme(shopId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.<ShopThemeResponse>builder()
                .code(201)
                .message("Create theme successfully")
                .result(response)
                .build());
    }

    /**
     * PUT: Update an existing theme configuration for a shop.
     * Only the shop owner can update the theme (BOLA protection).
     *
     * @param shopId the shop UUID
     * @param request the updated theme request DTO
     * @return updated theme response DTO
     */
    @PutMapping("/{shopId}/theme")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    @Operation(
        summary = "Update shop theme configuration",
        description = "Update an existing SDUI theme configuration for a shop. " +
                     "Only the fields provided in the request will be updated. " +
                     "Only the shop owner can update the theme."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Theme updated successfully",
            content = @Content(schema = @Schema(implementation = ShopThemeResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Theme not found for the shop"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - invalid data"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - user not authorized or not the shop owner"
        )
    })
    public ResponseEntity<ApiResponse<ShopThemeResponse>> updateTheme(
            Authentication authentication,
            @PathVariable
            @Parameter(description = "Shop UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            UUID shopId,

            @RequestBody
            @Valid
            @Parameter(description = "Updated theme configuration", required = true)
            ShopThemeRequest request) {
        validateThemeManagementPermission(authentication, shopId);

        log.info("PUT /api/v1/shops/{}/theme - Updating theme", shopId);
        ShopThemeResponse response = shopThemeService.updateTheme(shopId, request);

        return ResponseEntity.ok(ApiResponse.<ShopThemeResponse>builder()
            .code(200)
            .message("Update theme successfully")
            .result(response)
            .build());
    }

    /**
     * DELETE: Delete a theme configuration for a shop.
     * Only the shop owner can delete the theme (BOLA protection).
     *
     * @param shopId the shop UUID
     * @return success message
     */
    @DeleteMapping("/{shopId}/theme")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    @Operation(
        summary = "Delete shop theme configuration",
        description = "Delete the SDUI theme configuration for a shop. " +
                     "The shop will revert to default theme after deletion. " +
                     "Only the shop owner can delete the theme."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Theme deleted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Theme not found for the shop"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - user not authorized or not the shop owner"
        )
    })
    public ResponseEntity<ApiResponse<String>> deleteTheme(
            Authentication authentication,
            @PathVariable
            @Parameter(description = "Shop UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            UUID shopId) {
        validateThemeManagementPermission(authentication, shopId);

        log.info("DELETE /api/v1/shops/{}/theme - Deleting theme", shopId);
        shopThemeService.deleteThemeByShopId(shopId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
            .code(200)
            .message("Delete theme successfully")
            .result("Theme deleted")
            .build());
    }

    private void validateThemeManagementPermission(Authentication authentication, UUID shopId) {
        User operator = (User) authentication.getPrincipal();
        if (operator.getRole() == Role.VENDOR) {
            return;
        }
        if (operator.getRole() != Role.STAFF) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!staffPermissionService.hasPermission(operator.getId(), "SHOP_THEME:MANAGE")) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        String staffShopId = staffPermissionService.getStaffShopId(operator.getId());
        if (staffShopId == null || !shopId.toString().equals(staffShopId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}
