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

    @Schema(description = "ID của phiên Livestream (Nếu khách mua từ phòng Live)", example = "uuid-session-123", nullable = true)
    private String livestreamSessionId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @Schema(description = "ID sản phẩm", example = "product1")
        @NotNull(message = "productId không được để trống")
        private String productId;

        @Schema(description = "ID SKU da chon (optional)", example = "3f4d9f8a-2bcd-4f0b-8f53-97e5c9a41c92")
        private String skuId;

        @Schema(description = "Số lượng đặt cho sản phẩm", example = "2")
        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer quantity;
    }
}
