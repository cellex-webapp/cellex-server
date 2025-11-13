package com.example.cellex.dtos.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderFromCartRequest {

    @Schema(description = "Danh sách sản phẩm (productId và quantity) cần đặt")
    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @Schema(description = "ID sản phẩm", example = "product1")
        @NotNull(message = "productId không được để trống")
        private String productId;

        @Schema(description = "Số lượng đặt cho sản phẩm", example = "2")
        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer quantity;
    }
}
