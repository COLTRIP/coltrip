package com.coltrip.backend.domain.spot;

// 8종 확정(2026-08-18). 확장 가능 - docs/schema.md 참고
public enum Category {
    CAFE(100, SpotAreaType.POINT),
    PARK(250, SpotAreaType.AREA),
    LIBRARY(100, SpotAreaType.POINT),
    GALLERY(100, SpotAreaType.POINT),
    BOOKSTORE(100, SpotAreaType.POINT),
    TEMPLE(100, SpotAreaType.POINT),
    BEACH(250, SpotAreaType.AREA),
    ALLEY(250, SpotAreaType.AREA);

    private final int visitRadiusMeters;
    private final SpotAreaType areaType;

    Category(int visitRadiusMeters, SpotAreaType areaType) {
        this.visitRadiusMeters = visitRadiusMeters;
        this.areaType = areaType;
    }

    // 방문완료 판정용 반경. 점형 장소(카페 등) 100m, 면적형 장소(공원 등) 250m - 잠정값, 팀 확정 필요
    public int getVisitRadiusMeters() {
        return visitRadiusMeters;
    }

    public SpotAreaType getAreaType() {
        return areaType;
    }
}
