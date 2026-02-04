package com.example.corebanking.common;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        String status, // "SUCCESS" or "FAIL"
        String message, // "Request successful."
        @JsonInclude(JsonInclude.Include.NON_NULL) // Exclude from response if data is null
        T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", null, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("FAIL", message, null);
    }
}
