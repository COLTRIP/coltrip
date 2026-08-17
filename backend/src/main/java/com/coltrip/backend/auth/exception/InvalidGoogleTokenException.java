package com.coltrip.backend.auth.exception;

public class InvalidGoogleTokenException extends RuntimeException {

    public InvalidGoogleTokenException() {
        super("유효하지 않은 구글 idToken입니다.");
    }
}
