package com.coltrip.backend.internal.exception;

public class InvalidInternalApiKeyException extends RuntimeException {

    public InvalidInternalApiKeyException() {
        super("유효하지 않은 내부 API 키입니다.");
    }
}
