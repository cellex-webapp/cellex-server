package com.example.cellex.dtos.request.warranty;

import com.example.cellex.enums.WarrantyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatusUpdateRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private WarrantyStatus status;

    private String shopResponse;
}