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
 * Lưu trữ độ tương đồng giữa các sản phẩm (Item-based Collaborative Filtering)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_similarities")
@CompoundIndexes({
        @CompoundIndex(name = "product_similar_idx", def = "{'product_id': 1, 'similarity_score': -1}")
})
public class ProductSimilarity {

    @Id
    private String id;

    @Field("product_id")
    private String productId;

    @Field("similar_product_id")
    private String similarProductId;

    @Field("similarity_score")
    private Double similarityScore; // Cosine similarity (0-1)

    @Field("calculation_method")
    @Builder.Default
    private String calculationMethod = "COLLABORATIVE_FILTERING"; // CF, CONTENT_BASED, HYBRID

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
