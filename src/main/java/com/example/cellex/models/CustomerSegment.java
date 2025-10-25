package com.example.cellex.models;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customer_segments")
public class CustomerSegment {

    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("min_spend")
    private Double minSpend;

    @Field("max_spend")
    private Double maxSpend; // Optional, null nghĩa là không giới hạn trên

    @Field("level")
    private Integer level; // Cấp độ phân khúc (1, 2, 3...), càng cao càng VIP

    @Field("description")
    private String description;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}

