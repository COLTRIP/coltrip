package com.coltrip.backend.review.exception;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException() {
        super("존재하지 않는 리뷰입니다.");
    }
}
