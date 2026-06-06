package com.example.cellex.dtos.request.warranty;

import com.example.cellex.enums.WarrantyType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyPolicyRequest {
    @NotNull(message = "Thời gian bảo hành không được để trống")
    private Integer durationMonths;

    @NotNull(message = "Loại bảo hành không được để trống")
    private WarrantyType type;

    private String terms;
}