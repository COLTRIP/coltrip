package com.coltrip.backend.auth.exception;

public class UserNotRegisteredException extends RuntimeException {

    public UserNotRegisteredException() {
        super("가입되지 않은 사용자입니다.");
    }
}
