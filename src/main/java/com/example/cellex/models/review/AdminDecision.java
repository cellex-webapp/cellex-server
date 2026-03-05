package com.example.cellex.models.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDecision {

    private String adminId;
    private String adminName;
    private String action; // APPROVE, REJECT, HIDE
    private String reason;
    private LocalDateTime decidedAt;
}
