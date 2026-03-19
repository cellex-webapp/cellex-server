package com.example.cellex.models.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "livestream_comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivestreamCommentDocument {

    @Id
    private String id;

    @Field("session_id")
    private String sessionId;

    @Field("user_name")
    private String userName;

    private String content;

    @Field("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}