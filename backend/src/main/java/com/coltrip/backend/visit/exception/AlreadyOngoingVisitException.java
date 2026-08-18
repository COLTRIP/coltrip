package com.coltrip.backend.visit.exception;

public class AlreadyOngoingVisitException extends RuntimeException {

    public AlreadyOngoingVisitException() {
        super("이미 진행 중인 방문이 있습니다.");
    }
}
