package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "03. Address Management", description = "APIs for managing Vietnam address data")
@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @Operation(
            summary = "Get all provinces in Vietnam",
            description = "Retrieves the complete list of provinces/cities in Vietnam (updated 2024)"
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
            summary = "Get communes by province code",
            description = "Retrieves all communes/wards in a specific province (updated 2024)"
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
}
