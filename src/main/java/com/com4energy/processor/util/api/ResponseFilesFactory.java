package com.com4energy.processor.util.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.com4energy.processor.api.ApiMessages;
import com.com4energy.processor.api.ApiStatus;
import com.com4energy.processor.api.response.ApiResponse;

public class ResponseFilesFactory {

    public static <T> ResponseEntity<ApiResponse<T>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(ApiStatus.WARNING, message, null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(new ApiResponse<>(ApiStatus.SUCCESS, ApiMessages.SUCCESS, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> accepted(String message, T data) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(ApiStatus.SUCCESS, message, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(ApiStatus.ERROR, message, null));
    }

}
