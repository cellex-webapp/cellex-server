package com.example.cellex.dtos.request.segment;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerSegmentRequest {
    
    private String name;
    
    @Min(value = 0, message = "Mức chi tiêu tối thiểu phải >= 0")
    private Double minSpend;
    
    @Min(value = 0, message = "Mức chi tiêu tối đa phải >= 0")
    private Double maxSpend;
    
    @Min(value = 1, message = "Cấp độ phải >= 1")
    private Integer level;
    
    private String description;
}

