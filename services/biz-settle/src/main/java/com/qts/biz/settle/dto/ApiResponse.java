package com.qts.biz.settle.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 统一API响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private String requestId;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .requestId(java.util.UUID.randomUUID().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .requestId(java.util.UUID.randomUUID().toString())
                .build();
    }
}
