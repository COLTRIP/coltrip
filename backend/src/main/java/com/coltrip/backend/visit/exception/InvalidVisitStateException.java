package com.coltrip.backend.visit.exception;

public class InvalidVisitStateException extends RuntimeException {

    public InvalidVisitStateException() {
        super("이미 완료되었거나 취소된 방문입니다.");
    }
}
