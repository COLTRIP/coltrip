package com.coltrip.backend.spot.exception;

public class SpotNotFoundException extends RuntimeException {

    public SpotNotFoundException() {
        super("존재하지 않는 관광지입니다.");
    }
}
