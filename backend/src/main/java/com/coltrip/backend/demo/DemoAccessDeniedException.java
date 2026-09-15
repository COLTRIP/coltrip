package com.coltrip.backend.demo;

public class DemoAccessDeniedException extends RuntimeException {
    public DemoAccessDeniedException() {
        super("시연 환경 접근이 허용되지 않았습니다.");
    }
}
