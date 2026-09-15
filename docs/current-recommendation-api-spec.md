# 현재 고요지수 기준 추천 목록

## 범위

추천 화면 최초 진입 및 '현재' 복귀용 API다. 지도/상세와 동일한 tourist_spot 현재 점수 캐시를 사용한다. AI 호출, 예측 조회 또는 데이터 쓰기는 수행하지 않는다. 실제 실시간 관측값임을 보장하지 않으며 원천 데이터의 신선도와 저장 갱신 시각은 구분해야 한다.

기존 GET /api/spots/recommendations는 선택한 날짜/시간의 예측 전용으로 유지한다. 관측 타임라인 KST 수정, AI 점수 산식 조정, 프론트 화면 구현은 이 작업의 범위 밖이다.

위치기반서비스사업자 등록 이슈로 이 API는 위치를 전혀 받지 않는다(2026-09-15, 이슈 #115). 부산시청 기본 중심점/반경 개념을 제거했고, category/mode 필터에 부합하는 전체 장소를 대상으로 한다.

## 요청

```http
GET /api/spots/recommendations/current?category=BEACH&mode=SENSORY&limit=20
```

비로그인 허용. 유효한 액세스 토큰은 isLiked에 반영한다. 토큰 없음/만료/잘못된 토큰은 기존 공개 관광지 정책대로 익명 처리한다.

| 파라미터 | 필수 | 정책 |
|---|---|---|
| category | N | 기존 Category enum |
| mode | N | 기존 Mode enum |
| limit | N | 기본 20, 1~50 |

latitude/longitude/radiusMeters 파라미터는 없다. 날짜/시간은 이 API의 입력이 아니며 예측 모드로 전환하지 않는다.

DB에서 category/mode 후보를 조회한다. 점수 내림차순(null 마지막), ID 오름차순이다. 0점은 유효한 점수이며 null보다 앞선다. 이미지/모드/점수 없음만으로 장소를 제외하지 않는다. mode 필터가 있으면 해당 모드가 있는 장소만 선택하되 응답은 전체 모드를 반환한다.

## 응답

장소 데이터는 형식 설명용이다.

```json
{
  "type": "CURRENT",
  "timezone": "Asia/Seoul",
  "sort": "QUIET_DESC",
  "spots": [{
    "spot": {
      "id": 4,
      "name": "해운대해수욕장",
      "address": "부산광역시 예시 주소",
      "category": "BEACH",
      "modes": ["SENSORY", "VIBRANT"],
      "imageUrl": null,
      "latitude": 35.159084,
      "longitude": 129.1602786,
      "quietScore": 98,
      "quietLevel": "QUIET",
      "quietScoreUpdatedAt": "2026-09-14T22:05:00",
      "isLiked": false
    }
  }],
  "message": "현재 저장된 고요지수가 높은 순서입니다."
}
```

- spots[].spot은 지도 목록의 SpotSummaryResponse를 재사용한다. quietScore는 정수, quietLevel과 quietScoreUpdatedAt은 지도/상세와 같은 값이다. spot.latitude/longitude는 지도 표시용으로 그대로 남아있으나 필터/정렬에는 쓰이지 않는다.
- 미수신 점수/등급/시각은 null이다. 0점이나 예측값으로 대체하지 않는다. 갱신 시각은 기존 API처럼 KST LocalDateTime 문자열로 반환한다.
- forecast/targetAt/generatedAt 필드는 없다. 현재 모드는 최상위 type=CURRENT로 구분한다.
- 결과 없음은 200, spots=[], message="현재 조건에 맞는 장소가 없습니다."다. 모든 장소의 점수가 null이어도 조건에 맞는 장소는 반환한다.
- Cache-Control: no-store로 사용자별 좋아요 응답의 공유 캐시를 방지한다.
- 별도 HTTP 요청 사이 동기화가 발생하면 지도/상세와 점수가 달라질 수 있다. 동일 저장 상태에서 일치하는 계약이다.

| HTTP | code | 조건 |
|---|---|---|
| 400 | InvalidCurrentRecommendationRequest | limit 범위 오류 |
| 400 | InvalidParameterException | 숫자/enum 파싱 오류 |
| 500 | InternalServerError | 예상하지 못한 서버/DB 오류 |

예측 API의 기존 InvalidForecastRequest 응답은 변경하지 않는다.

## 프론트 연동

- 최초 진입/현재 복귀: 이 API 호출, spots[].spot.quietScore 사용.
- 날짜/시간 선택: 기존 예측 API 호출, spots[].forecast.quietIndex 사용.
- '오늘' 여부가 아니라 명시적인 현재/예측 모드로 호출을 구분한다.
- 모드 전환 시 이전 요청의 늦은 응답이 새 목록을 덮어쓰지 않도록 처리한다.
- 현재 목록에 '현재 고요지수'와 갱신 시각, 예측 목록에 대상 날짜/시간을 표시한다. 미수신 값은 0점으로 표시하지 않는다.
- 프론트가 새 API로 호출을 바꾸기 전까지 기존 화면은 계속 예측 목록을 사용한다.

## 검증 및 운영

CurrentRecommendationIntegrationTest와 CurrentRecommendationServiceTest는 현재/지도/상세 점수 일치, null/0점 정렬, ID 동점, 필터/전체 모드, 인증별 좋아요, 빈 결과와 입력 검증을 확인한다. 기존 Forecast 테스트도 함께 실행한다.

신규 DB 컬럼/마이그레이션은 없다. 조회는 읽기 전용이다. 위치 필터가 없어졌으므로 장소 수 증가 시 category/mode 조건만으로 전체 스캔에 가까워질 수 있다 — 데이터 증가 시 실행 계획 검증이 필요하다. 운영 MySQL 및 실제 프론트 연동 검증은 별도로 진행한다.
