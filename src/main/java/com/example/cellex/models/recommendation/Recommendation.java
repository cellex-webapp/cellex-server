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
 * Lưu kết quả gợi ý cho từng user (pre-computed offline)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recommendations")
@CompoundIndexes({
        @CompoundIndex(name = "user_score_idx", def = "{'user_id': 1, 'recommendation_score': -1}")
})
public class Recommendation {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("product_id")
    private String productId;

    @Field("recommendation_score")
    private Double recommendationScore; // Điểm gợi ý (0-1)

    @Field("recommendation_reason")
    private String recommendationReason; // CF, TRENDING, CONTENT_BASED, POPULARITY

    @Field("explanation")
    private String explanation; // Giải thích tại sao gợi ý (explainable AI)

    @Field("rank")
    private Integer rank; // Thứ hạng trong danh sách gợi ý của user

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
