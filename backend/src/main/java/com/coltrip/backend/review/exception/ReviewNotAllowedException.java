package com.coltrip.backend.review.exception;

public class ReviewNotAllowedException extends RuntimeException {

    public ReviewNotAllowedException(String message) {
        super(message);
    }
}
