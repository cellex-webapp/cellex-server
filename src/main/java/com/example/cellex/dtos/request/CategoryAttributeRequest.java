package com.example.cellex.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request để tạo hoặc cập nhật thuộc tính danh mục")
public class CategoryAttributeRequest {

    @NotBlank(message = "Tên thuộc tính không được để trống")
    @Schema(description = "Tên hiển thị của thuộc tính",
            example = "Dung lượng RAM",
            required = true)
    private String attributeName;

    @NotBlank(message = "Key thuộc tính không được để trống")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Key chỉ được chứa chữ thường, số và dấu gạch dưới")
    @Schema(description = "Key duy nhất của thuộc tính (dùng trong code, chỉ chứa chữ thường, số và dấu gạch dưới)",
            example = "ram_capacity",
            required = true,
            pattern = "^[a-z0-9_]+$")
    private String attributeKey;

    @NotBlank(message = "Kiểu dữ liệu không được để trống")
    @Pattern(regexp = "^(TEXT|NUMBER|BOOLEAN|SELECT|MULTI_SELECT)$",
            message = "Kiểu dữ liệu phải là: TEXT, NUMBER, BOOLEAN, SELECT hoặc MULTI_SELECT")
    @Schema(description = "Kiểu dữ liệu của thuộc tính",
            example = "SELECT",
            required = true,
            allowableValues = {"TEXT", "NUMBER", "BOOLEAN", "SELECT", "MULTI_SELECT"},
            implementation = String.class)
    private String dataType;

    @Schema(description = "Đơn vị đo lường (nếu có)",
            example = "GB",
            required = false)
    private String unit;

    @NotNull(message = "Trạng thái bắt buộc không được null")
    @Schema(description = "Có phải là thuộc tính bắt buộc khi tạo sản phẩm hay không",
            example = "true",
            required = true)
    private Boolean isRequired;

    @NotNull(message = "Trạng thái có thể lọc không được null")
    @Schema(description = "Có thể sử dụng để lọc sản phẩm hay không",
            example = "true",
            required = true)
    private Boolean isFilterable;

    @NotNull(message = "Trạng thái có thể so sánh không được null")
    @Schema(description = "Có thể sử dụng để so sánh sản phẩm hay không",
            example = "true",
            required = true)
    private Boolean isComparable;

    @NotNull(message = "Trạng thái thông số nổi bật không được null")
    @Schema(description = "Có phải là thông số nổi bật hiển thị trên card sản phẩm hay không",
            example = "true",
            required = true)
    private Boolean isHighlight;

    @Schema(description = "Danh sách các lựa chọn (chỉ áp dụng cho dataType = SELECT hoặc MULTI_SELECT)",
            example = "[\"4GB\", \"8GB\", \"16GB\", \"32GB\"]",
            required = false)
    private List<String> selectOptions;

    @Schema(description = "Pattern validation cho thuộc tính (regex pattern)",
            example = "^[0-9]+$",
            required = false)
    private String validationPattern;

    @Schema(description = "Thứ tự sắp xếp hiển thị",
            example = "1",
            required = false)
    private Integer sortOrder;

    @Schema(description = "Mô tả chi tiết về thuộc tính",
            example = "Dung lượng RAM của thiết bị, ảnh hưởng đến hiệu năng đa nhiệm",
            required = false)
    private String description;
}
