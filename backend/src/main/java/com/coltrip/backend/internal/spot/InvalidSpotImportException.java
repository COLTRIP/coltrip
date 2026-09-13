package com.coltrip.backend.internal.spot;

public class InvalidSpotImportException extends RuntimeException {
    public InvalidSpotImportException(String message) {
        super(message);
    }
}
