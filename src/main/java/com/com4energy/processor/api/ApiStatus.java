package com.com4energy.processor.api;

public enum ApiStatus {
    SUCCESS, ERROR, WARNING;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

}
