package com.coltrip.backend.common.exception;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(RuntimeException e) {
        return new ErrorResponse(e.getClass().getSimpleName(), e.getMessage());
    }
}
