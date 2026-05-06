package com.example.cellex.dtos.request.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
public class InventoryImportRequest {

    private String shopId;

    @NotBlank(message = "supplierId khong duoc de trong")
    private String supplierId;

    @NotEmpty(message = "Danh sach item khong duoc de trong")
    @Valid
    private List<Item> items;

    private String note;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @NotBlank(message = "skuId khong duoc de trong")
        private String skuId;

        @NotNull(message = "quantity khong duoc de trong")
        @Min(value = 1, message = "quantity phai lon hon 0")
        private Integer quantity;

        @NotNull(message = "importPrice khong duoc de trong")
        @DecimalMin(value = "0.0", inclusive = false, message = "importPrice phai lon hon 0")
        private Double importPrice;
    }
}
