package com.example.cellex.models.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Field("product_id")
    private String productId;

    @Field("product_name")
    private String productName;

    @Field("product_image")
    private String productImage;

    @Field("price")
    private Double price; // Giá tại thời điểm đặt hàng

    @Field("quantity")
    private Integer quantity;

    @Field("subtotal")
    private Double subtotal; // price * quantity
}

