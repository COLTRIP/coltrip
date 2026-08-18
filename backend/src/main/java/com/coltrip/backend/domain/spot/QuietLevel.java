package com.coltrip.backend.domain.spot;

// 100점 만점 quietScore 기준 구간 (2026-08-18 확정): 0~40 혼잡, 41~70 보통, 71~100 고요
public enum QuietLevel {
    CROWDED, NORMAL, QUIET;

    public static QuietLevel from(Integer quietScore) {
        if (quietScore == null) {
            return null;
        }
        if (quietScore <= 40) {
            return CROWDED;
        }
        if (quietScore <= 70) {
            return NORMAL;
        }
        return QUIET;
    }
}
