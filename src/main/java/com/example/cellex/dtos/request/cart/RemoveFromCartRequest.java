package com.example.cellex.dtos.request.cart;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemoveFromCartRequest {

    @NotEmpty(message = "Danh sách product IDs không được để trống")
    private List<String> productIds;
}

