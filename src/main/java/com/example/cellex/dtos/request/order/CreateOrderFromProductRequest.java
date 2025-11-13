package com.example.cellex.dtos.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderFromProductRequest {

    @Schema(description = "ID sản phẩm", example = "product123")
    @NotBlank(message = "ID sản phẩm không được để trống")
    private String productId;

    @Schema(description = "Số lượng", example = "2")
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    // Note removed: note should be provided at checkout via CheckoutOrderRequest
}
