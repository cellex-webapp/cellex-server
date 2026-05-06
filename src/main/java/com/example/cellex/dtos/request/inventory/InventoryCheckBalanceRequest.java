package com.example.cellex.dtos.request.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckBalanceRequest {

    private String shopId;

    @NotEmpty(message = "Danh sach item khong duoc de trong")
    @Valid
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @NotBlank(message = "skuId khong duoc de trong")
        private String skuId;

        @NotNull(message = "actualStock khong duoc de trong")
        @Min(value = 0, message = "actualStock khong duoc am")
        private Integer actualStock;

        private String reason;
    }
}
