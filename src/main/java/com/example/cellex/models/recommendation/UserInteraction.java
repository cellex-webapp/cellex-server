package com.example.cellex.models.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Lưu tương tác của user với sản phẩm (view, cart, purchase, review)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_interactions")
@CompoundIndexes({
        @CompoundIndex(name = "user_product_idx", def = "{'user_id': 1, 'product_id': 1}"),
        @CompoundIndex(name = "user_updated_idx", def = "{'user_id': 1, 'updated_at': -1}")
})
public class UserInteraction {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("product_id")
    private String productId;

    @Field("category_id")
    private String categoryId;

    // Điểm tương tác (weighted score)
    @Field("view_count")
    @Builder.Default
    private Integer viewCount = 0; // Trọng số: 1

    @Field("cart_count")
    @Builder.Default
    private Integer cartCount = 0; // Trọng số: 3

    @Field("purchase_count")
    @Builder.Default
    private Integer purchaseCount = 0; // Trọng số: 5

    @Field("review_count")
    @Builder.Default
    private Integer reviewCount = 0; // Trọng số: 4

    @Field("total_score")
    @Builder.Default
    private Double totalScore = 0.0; // Tổng điểm (calculated)

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    // Tính tổng điểm tương tác
    public void calculateTotalScore() {
        this.totalScore = (viewCount * 1.0) + 
                         (cartCount * 3.0) + 
                         (purchaseCount * 5.0) + 
                         (reviewCount * 4.0);
    }
}
