package com.example.cellex.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSegmentResponse {
    private String id;
    private String name;
    private Double minSpend;
    private Double maxSpend;
    private Integer level;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

