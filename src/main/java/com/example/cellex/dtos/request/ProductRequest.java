package com.example.cellex.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Danh mục không được để trống")
    private String categoryId;

    @NotBlank(message = "Mô tả ngắn không được để trống")
    private String shortDescription;

    private String description;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", message = "Giá sản phẩm phải lớn hơn 0")
    private Double price;

    @DecimalMin(value = "0.0", message = "Phần trăm giảm giá phải từ 0")
    @DecimalMax(value = "100.0", message = "Phần trăm giảm giá không được vượt quá 100")
    private Double saleOff;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho phải từ 0 trở lên")
    private Integer stockQuantity;

    @DecimalMin(value = "0.0", message = "Trọng lượng phải lớn hơn 0")
    private Double weight;

    @NotEmpty(message = "Phải có ít nhất một hình ảnh")
    private List<String> images;

    @Valid
    @NotEmpty(message = "Phải có thông tin thuộc tính sản phẩm")
    private List<ProductAttributeValueRequest> attributeValues;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductAttributeValueRequest {

        @NotBlank(message = "ID thuộc tính không được để trống")
        private String attributeId;

        @NotBlank(message = "Giá trị thuộc tính không được để trống")
        private String value;
    }
}
