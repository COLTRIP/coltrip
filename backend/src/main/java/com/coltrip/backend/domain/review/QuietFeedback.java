package com.coltrip.backend.domain.review;

// 별점 대신 "기대한 만큼 조용했는가"를 묻는다.
// 기획서 차별점(별점/후기 중심이 아닌 고요함 중심)과 일관되며,
// 추후 AI 고요지수 실측 보정 데이터로도 활용 가능.
public enum QuietFeedback {
    QUIETER_THAN_EXPECTED,
    AS_EXPECTED,
    NOISIER_THAN_EXPECTED
}
