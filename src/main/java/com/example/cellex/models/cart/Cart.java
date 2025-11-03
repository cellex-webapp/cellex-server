package com.example.cellex.models.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "carts")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Cart {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    @Field("items")
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Field("total_price")
    @Builder.Default
    private Double totalPrice = 0.0;

    @Field("total_quantity")
    @Builder.Default
    private Integer totalQuantity = 0;

    @Field("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CartItem {
        @Field("product_id")
        private String productId;

        @Field("quantity")
        private Integer quantity;

        @Field("price")
        private Double price;

        @Field("product_name")
        private String productName;

        @Field("product_image")
        private String productImage;

        @Field("shop_id")
        private String shopId;

        @Field("shop_name")
        private String shopName;
    }
}

