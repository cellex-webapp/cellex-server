package com.example.cellex.dtos.request.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserSpendRequest {
    
    @NotBlank(message = "User ID không được để trống")
    private String userId;
    
    @NotNull(message = "Số tiền không được để trống")
    @Min(value = 0, message = "Số tiền phải >= 0")
    private Double amount;
}

