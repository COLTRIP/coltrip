package com.coltrip.backend.internal.exception;

public class InvalidObservationTimeException extends RuntimeException {
    public InvalidObservationTimeException() {
        super("calculatedAt은 한국 시간 기준 현재 또는 과거의 관측 시각이어야 합니다.");
    }
}
