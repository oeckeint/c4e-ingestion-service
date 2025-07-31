package com.com4energy.processor.api.response;

import com.com4energy.processor.api.ApiStatus;

public record ApiResponse<T> (ApiStatus status, String message, T data) {}
