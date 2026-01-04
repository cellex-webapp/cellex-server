package com.example.cellex.controllers;

import com.example.cellex.dtos.request.address.WardMappingRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.address.*;
import com.example.cellex.models.address.Commune;
import com.example.cellex.models.address.Province;
import com.example.cellex.services.address.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "03. Address Management", description = "APIs for managing Vietnam address data with dual system support (before/after 07/2025)")
@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // ==================== Legacy APIs (Current System) ====================
    
    @Operation(
            summary = "Get all provinces in Vietnam (Legacy)",
            description = "Retrieves the complete list of provinces/cities in Vietnam using legacy data format"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Provinces retrieved successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)) }
            )
    })
    @GetMapping("/provinces")
    public ApiResponse<List<Province>> getAllProvinces() {
        List<Province> provinces = addressService.getAllProvinces();
        return ApiResponse.<List<Province>>builder()
                .result(provinces)
                .message("Provinces retrieved successfully.")
                .build();
    }

    @Operation(
            summary = "Get communes by province code (Legacy)",
            description = "Retrieves all communes/wards in a specific province using legacy data format"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Communes retrieved successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)) }
            )
    })
    @GetMapping("/communes/{provinceCode}")
    public ApiResponse<List<Commune>> getCommunesByProvince(
            @Parameter(description = "Province code", example = "01")
            @PathVariable String provinceCode) {
        List<Commune> communes = addressService.getCommunesByProvinceCode(provinceCode);
        return ApiResponse.<List<Commune>>builder()
                .result(communes)
                .message("Communes retrieved successfully.")
                .build();
    }

    // ==================== Old Address System APIs (Before 07/2025) ====================
    
    @Operation(
            summary = "Get all provinces (Old System - Before 07/2025)",
            description = "Retrieves provinces from the old 3-level address system (Province -> District -> Ward)"
    )
    @GetMapping("/old/provinces")
    public ApiResponse<List<OldProvinceResponse>> getOldProvinces() {
        List<OldProvinceResponse> provinces = addressService.getOldProvinces();
        return ApiResponse.<List<OldProvinceResponse>>builder()
                .result(provinces)
                .message("Old provinces retrieved successfully.")
                .build();
    }

    @Operation(
            summary = "Get districts by province (Old System - Before 07/2025)",
            description = "Retrieves districts/quận/huyện for a specific province from the old address system"
    )
    @GetMapping("/old/districts")
    public ApiResponse<List<OldDistrictResponse>> getOldDistrictsByProvince(
            @Parameter(description = "Province ID from old system", example = "01")
            @RequestParam("provinceId") String provinceId) {
        List<OldDistrictResponse> districts = addressService.getOldDistrictsByProvince(provinceId);
        return ApiResponse.<List<OldDistrictResponse>>builder()
                .result(districts)
                .message("Old districts retrieved successfully.")
                .build();
    }

    @Operation(
            summary = "Get wards by district (Old System - Before 07/2025)",
            description = "Retrieves wards/phường/xã for a specific district from the old address system"
    )
    @GetMapping("/old/wards")
    public ApiResponse<List<OldWardResponse>> getOldWardsByDistrict(
            @Parameter(description = "District ID from old system", example = "001")
            @RequestParam("districtId") String districtId) {
        List<OldWardResponse> wards = addressService.getOldWardsByDistrict(districtId);
        return ApiResponse.<List<OldWardResponse>>builder()
                .result(wards)
                .message("Old wards retrieved successfully.")
                .build();
    }

    // ==================== New Address System APIs (After 07/2025) ====================
    
    @Operation(
            summary = "Get all provinces (New System - After 07/2025)",
            description = "Retrieves provinces from the new 2-level address system (Province -> Ward, no district level)"
    )
    @GetMapping("/new/provinces")
    public ApiResponse<List<NewProvinceResponse>> getNewProvinces() {
        List<NewProvinceResponse> provinces = addressService.getNewProvinces();
        return ApiResponse.<List<NewProvinceResponse>>builder()
                .result(provinces)
                .message("New provinces retrieved successfully.")
                .build();
    }

    @Operation(
            summary = "Get wards by province (New System - After 07/2025)",
            description = "Retrieves wards directly under a province (no district level) from the new address system"
    )
    @GetMapping("/new/wards")
    public ApiResponse<List<NewWardResponse>> getNewWardsByProvince(
            @Parameter(description = "Province code from new system", example = "01")
            @RequestParam("provinceCode") String provinceCode) {
        List<NewWardResponse> wards = addressService.getNewWardsByProvince(provinceCode);
        return ApiResponse.<List<NewWardResponse>>builder()
                .result(wards)
                .message("New wards retrieved successfully.")
                .build();
    }

    // ==================== Ward Mapping API ====================
    
    @Operation(
            summary = "Map ward code between old and new systems",
            description = "Converts a ward code from old system to new system or vice versa. " +
                    "Automatically detects the input type if not specified. " +
                    "When converting from new to old, returns all old wards that merged into the new ward."
    )
    @PostMapping("/map")
    public ApiResponse<WardMappingResponse> mapWardCode(
            @Valid @RequestBody WardMappingRequest request) {
        WardMappingResponse response = addressService.mapWardCode(request.getWardCode(), request.getCodeType());
        if (response == null) {
            return ApiResponse.<WardMappingResponse>builder()
                    .code(404)
                    .message("Ward code not found in mapping table.")
                    .build();
        }
        return ApiResponse.<WardMappingResponse>builder()
                .result(response)
                .message("Ward mapping retrieved successfully.")
                .build();
    }

    // ==================== Dual Address Display API ====================
    
    @Operation(
            summary = "Get dual address display",
            description = "Builds complete address information in both old and new formats from stored new ward code and detail address"
    )
    @GetMapping("/dual")
    public ApiResponse<DualAddressResponse> getDualAddress(
            @Parameter(description = "New ward code (stored in database)", example = "00004")
            @RequestParam("newWardCode") String newWardCode,
            @Parameter(description = "Detail address (street, house number, etc.)", example = "123 Đường ABC")
            @RequestParam(value = "detailAddress", required = false) String detailAddress) {
        DualAddressResponse response = addressService.buildDualAddress(newWardCode, detailAddress);
        if (response == null) {
            return ApiResponse.<DualAddressResponse>builder()
                    .code(404)
                    .message("Ward code not found.")
                    .build();
        }
        return ApiResponse.<DualAddressResponse>builder()
                .result(response)
                .message("Dual address retrieved successfully.")
                .build();
    }
}
