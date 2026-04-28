package com.example.cellex.dtos.request.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuRequest {

    private String skuCode;

    @NotEmpty(message = "variationData khong duoc de trong")
    private Map<String, String> variationData;

    private String imageUrl;

    @NotNull(message = "price khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "price phai lon hon 0")
    private Double price;

    @Builder.Default
    @Min(value = 0, message = "onHandStock khong duoc am")
    private Integer onHandStock = 0;

    @Builder.Default
    @Min(value = 0, message = "reservedStock khong duoc am")
    private Integer reservedStock = 0;

    @Builder.Default
    @Min(value = 0, message = "safetyStock khong duoc am")
    private Integer safetyStock = 0;

    @Builder.Default
    private Boolean isActive = true;
}
