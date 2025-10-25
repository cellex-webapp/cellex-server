package com.example.cellex.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerSegmentRequest {
    
    @NotBlank(message = "Tên phân khúc không được để trống")
    private String name;
    
    @NotNull(message = "Mức chi tiêu tối thiểu không được để trống")
    @Min(value = 0, message = "Mức chi tiêu tối thiểu phải >= 0")
    private Double minSpend;
    
    @Min(value = 0, message = "Mức chi tiêu tối đa phải >= 0")
    private Double maxSpend; // Optional
    
    @NotNull(message = "Cấp độ không được để trống")
    @Min(value = 1, message = "Cấp độ phải >= 1")
    private Integer level;
    
    private String description;
}

