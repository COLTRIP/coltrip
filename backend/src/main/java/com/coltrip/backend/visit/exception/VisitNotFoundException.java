package com.coltrip.backend.visit.exception;

public class VisitNotFoundException extends RuntimeException {

    public VisitNotFoundException() {
        super("존재하지 않는 방문입니다.");
    }
}
