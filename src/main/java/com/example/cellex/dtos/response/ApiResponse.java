package com.example.cellex.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ serialize các trường không null
public class ApiResponse<T> {
    @Builder.Default
    private int code = 1000; // Mã thành công mặc định
    private String message;
    private T result;
}