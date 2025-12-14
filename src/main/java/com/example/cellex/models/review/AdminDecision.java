package com.example.cellex.models.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDecision {

    @Field("admin_id")
    private String adminId;

    @Field("admin_name")
    private String adminName;

    @Field("action")
    private String action; // APPROVE, REJECT, HIDE

    @Field("reason")
    private String reason;

    @Field("decided_at")
    private LocalDateTime decidedAt;
}
