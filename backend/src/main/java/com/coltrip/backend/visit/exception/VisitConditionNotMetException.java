package com.coltrip.backend.visit.exception;

public class VisitConditionNotMetException extends RuntimeException {

    public VisitConditionNotMetException() {
        super("목적지 반경 또는 체류시간 조건을 충족하지 않았습니다.");
    }
}
