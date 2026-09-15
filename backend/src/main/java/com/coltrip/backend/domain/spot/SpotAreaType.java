package com.coltrip.backend.domain.spot;

// 방문완료 반경 판정 기준의 구분. 점형(카페 등)은 좁은 반경, 면적형(공원 등)은 넓은 반경을 쓴다.
public enum SpotAreaType {
    POINT,
    AREA
}
