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
public class VendorResponse {

    private String vendorId;
    private String vendorName;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
