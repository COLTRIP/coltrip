package com.coltrip.backend.domain.spot;

// 8종 확정(2026-08-18). 확장 가능 - docs/schema.md 참고
public enum Category {
    CAFE(100),
    PARK(250),
    LIBRARY(100),
    GALLERY(100),
    BOOKSTORE(100),
    TEMPLE(100),
    BEACH(250),
    ALLEY(250);

    private final int visitRadiusMeters;

    Category(int visitRadiusMeters) {
        this.visitRadiusMeters = visitRadiusMeters;
    }

    // 방문완료 판정용 반경. 점형 장소(카페 등) 100m, 면적형 장소(공원 등) 250m - 잠정값, 팀 확정 필요
    public int getVisitRadiusMeters() {
        return visitRadiusMeters;
    }
}
