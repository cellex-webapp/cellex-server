package com.example.cellex.models.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "products")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    private String id;

    private String shopId;

    private String categoryId;

    private String name;

    private String description;

    private List<String> images;

    private Double price;

    private Double saleOff; // Phần trăm giảm giá (0-100)

    private Double finalPrice;

    private Integer stockQuantity;

    // Thay thế attributes và specifications cũ bằng cấu trúc mới
    private List<ProductAttributeValue> attributeValues;

    // Các thông tin thống kê
    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    @Builder.Default
    private Integer purchaseCount = 0;

    @Builder.Default
    private Boolean isPublished = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Inner class để lưu giá trị thuộc tính
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductAttributeValue {
        private String attributeId; // Tham chiếu đến CategoryAttribute
        private String attributeKey; // Key của thuộc tính (để tìm kiếm nhanh)
        private String attributeName; // Tên thuộc tính (để hiển thị)
        private String value; // Giá trị của thuộc tính
        private String unit; // Đơn vị
        private String dataType; // Kiểu dữ liệu
    }
}
