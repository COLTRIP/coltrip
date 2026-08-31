package com.coltrip.backend.auth.exception;

public class AlreadyRegisteredUserException extends RuntimeException {

    public AlreadyRegisteredUserException() {
        super("이미 가입된 사용자입니다.");
    }
}
