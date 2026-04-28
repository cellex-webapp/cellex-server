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

    @Schema(description = "ID SKU da chon (optional)", example = "3f4d9f8a-2bcd-4f0b-8f53-97e5c9a41c92")
    private String skuId;

    @Schema(description = "Số lượng", example = "2")
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @Schema(description = "ID của phiên Livestream (Nếu khách mua từ phòng Live)", example = "uuid-session-123", nullable = true)
    private String livestreamSessionId;

    // Note removed: note should be provided at checkout via CheckoutOrderRequest
}
