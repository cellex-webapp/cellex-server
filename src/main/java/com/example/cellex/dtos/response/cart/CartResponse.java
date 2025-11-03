package com.example.cellex.dtos.response.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {

    private String id;
    private String userId;
    private List<CartItemResponse> items;
    private Double totalPrice;
    private Integer totalQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CartItemResponse {
        private String productId;
        private String productName;
        private String productImage;
        private Integer quantity;
        private Double price;
        private Double subtotal; // quantity * price
        private String shopId;
        private String shopName;
        private Integer availableStock;
        private Boolean isAvailable;
    }
}

