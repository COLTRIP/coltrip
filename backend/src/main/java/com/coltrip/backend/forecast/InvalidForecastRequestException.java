package com.coltrip.backend.forecast;

public class InvalidForecastRequestException extends RuntimeException {
    public InvalidForecastRequestException(String message) {
        super(message);
    }
}
