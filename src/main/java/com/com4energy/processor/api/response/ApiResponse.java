package com.com4energy.processor.api.response;

import com.com4energy.processor.api.ApiStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiResponse<T> (ApiStatus status, String message, @JsonProperty("files") T data) {}
