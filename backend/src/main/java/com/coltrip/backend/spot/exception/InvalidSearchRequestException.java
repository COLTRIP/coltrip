package com.coltrip.backend.spot.exception;

public class InvalidSearchRequestException extends RuntimeException {
    public InvalidSearchRequestException(String message) {
        super(message);
    }
}
