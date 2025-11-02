package com.example.cellex.models.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "category_attributes")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryAttribute {

    @Id
    private String id;

    private String categoryId;

    private String attributeName; // Tên thuộc tính (VD: "RAM", "Chipset", "Camera chính")

    private String attributeKey; // Key để code sử dụng (VD: "ram", "chipset", "main_camera")

    private String dataType; // "TEXT", "NUMBER", "BOOLEAN", "SELECT", "MULTI_SELECT"

    private String unit; // Đơn vị (VD: "GB", "MP", "inch")

    private Boolean isRequired; // Bắt buộc nhập hay không

    private Boolean isHighlight; // Có phải là thông số nổi bật không (hiển thị trên card sản phẩm)

    private List<String> selectOptions; // Các lựa chọn nếu dataType là SELECT hoặc MULTI_SELECT

    private String validationPattern; // Regex để validate (nếu cần)

    private Integer sortOrder; // Thứ tự hiển thị

    private String description; // Mô tả thuộc tính

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
