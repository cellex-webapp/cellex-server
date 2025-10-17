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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Builder.Default
    private int code = 200;
    private String message;
    private T result;

    // Manual static methods để thay thế builder khi Lombok không hoạt động
    public static <T> ApiResponse<T> success(String message, T result) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = 1000;
        response.message = message;
        response.result = result;
        return response;
    }

}