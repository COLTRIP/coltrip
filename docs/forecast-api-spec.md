# 날짜/시간대별 추천 및 예측 고요지수 API

## 범위와 선행 조건

현재 코드의 시간대별 AI 조회는 hour/isWeekend로 점수를 계산하지만 특정 날짜의 예측 대상 시각·생성 시각을 반환하는 계약이 아니다. 이 명세는 **백엔드에 새로 제안한 예측 데이터 계약**이며 AI가 이미 전송하고 있는 형식이라고 간주하지 않는다.

기존 문서의 AI→백엔드 push 방식에 맞춰 예측 전용 수신 API를 추가한다. AI팀은 이 필드들을 실제 모델 출력에서 생성해 보내거나, 다른 확정된 응답이 있다면 명세를 조정해야 한다. 백엔드가 현재 점수에 미래 날짜를 붙여 예측값으로 만들어서는 안 된다.

예측 미수신 상태에서도 추천 API는 정상적으로 200과 빈 목록을 반환한다. 이는 AI 연동 완료를 의미하지 않는다. 예측 모델과 실제 데이터 전송은 이번 백엔드 코드 밖의 선행 조건이다.

## 기존 기능과 분리

| 기능 | 저장소 | 응답 의미 |
|---|---|---|
| 기존 지도/장소 상세 | tourist_spot의 현재 점수 캐시 | 기존 현재 점수 |
| 기존 /quiet-index/timeline | quiet_index | 최근 24시간 관측 이력 |
| 신규 /recommendations 및 /quiet-index/forecast | quiet_forecast | 특정 대상 시각의 예측 |

기존 GET /api/spots와 GET /api/spots/{spotId}, 최근 24시간 관측 타임라인의 주소/응답은 변경하지 않는다. 추천 목록의 forecast와 예측 타임라인의 슬롯은 동일한 Point DTO와 테이블, 최신 버전 선택 기준을 사용한다.

## 시간 정책

- API 기준 시간대는 Asia/Seoul.
- 조회 입력은 date=YYYY-MM-DD, hour=0~23. 1시간 슬롯의 시작 시각을 의미한다.
- 조회 범위는 현재 시간 슬롯부터 7일 뒤 같은 시간 슬롯까지. 과거 슬롯은 400.
- 현재 시간이 12:30이면 오늘 hour=12는 허용하지만 hour=11은 거부한다.
- 예측 타임라인은 요청 시작부터 24개 슬롯 전체가 위 범위 안에 있어야 한다.
- 7일은 백엔드 제안 정책이며 AI의 실제 예측 가능 기간이 확정되면 맞춰 변경한다.
- 응답 시각은 +09:00 오프셋을 포함한다. 입력 수신 시각도 UTC 오프셋이 필요하다.
- DB 시각은 한국 시간 LocalDateTime으로 정규화하며 DATETIME(6) 수준인 마이크로초로 저장한다.
- 날짜 선택은 주말 여부만 고르는 것과 다르다. 기존 AI의 hour/isWeekend를 특정 날짜 예측으로 둔갑시키지 않는다.

## GET /api/spots/recommendations

비로그인 허용. 유효한 토큰이 있으면 isLiked에 본인의 좋아요를 반영한다.

예시:

```text
GET /api/spots/recommendations?date=2026-09-14&hour=15&category=PARK&mode=NATURAL&limit=20
```

예시 날짜는 실행 시점의 허용 범위에 맞춰 바꿔 사용한다.

| 파라미터 | 필수 | 정책 |
|---|---|---|
| date | Y | 한국 날짜 YYYY-MM-DD |
| hour | Y | 0~23 정수 |
| category | N | 기존 Category enum |
| mode | N | 기존 Mode enum |
| limit | N | 기본 20, 허용 1~50 |

위치기반서비스사업자 등록 이슈로 이 API는 위치를 전혀 받지 않는다(2026-09-15, 이슈 #115). latitude/longitude/radiusMeters 파라미터는 없으며, 부산시청 기본 중심점/반경 개념도 없다. category/mode 필터에 부합하는 전체 장소를 대상으로 한다.

정렬은 예측 점수 내림차순 → 장소 ID 오름차순으로 고정한다. 이는 감성 임베딩 점수 기반 개인화 순위가 아니라 선택한 감성모드/카테고리 안에서의 예측 고요지수 순위다.

Category: CAFE, PARK, LIBRARY, GALLERY, BOOKSTORE, TEMPLE, BEACH, ALLEY.

Mode: COZY, NATURAL, URBAN, VINTAGE, EXOTIC, VIBRANT, SENSORY, TRANQUIL. mode는 필터일 뿐 응답 modes에는 장소의 전체 감성모드가 담긴다.

### 응답 예시

아래 장소와 점수는 형식 설명용이다.

```json
{
  "timezone": "Asia/Seoul",
  "targetAt": "2026-09-14T15:00:00+09:00",
  "sort": "QUIET_DESC",
  "spots": [
    {
      "spot": {
        "id": 42,
        "name": "예시 공원",
        "address": "부산광역시",
        "category": "PARK",
        "modes": ["NATURAL", "SENSORY"],
        "imageUrl": null,
        "latitude": 35.18,
        "longitude": 129.08,
        "isLiked": false
      },
      "forecast": {
        "type": "FORECAST",
        "targetAt": "2026-09-14T15:00:00+09:00",
        "quietIndex": 82.35,
        "generatedAt": "2026-09-13T12:00:00+09:00",
        "validUntil": "2026-09-14T16:00:00+09:00",
        "source": "coltrip-ai",
        "modelVersion": "forecast-v1"
      }
    }
  ],
  "message": "예측 고요지수가 높은 순서입니다."
}
```

예측이 없는 장소, 만료된 예측, 필터 밖의 장소는 추천 목록에서 제외한다. 결과가 없으면 spots=[]와 '선택한 시간과 조건에 맞는 유효한 예측 데이터가 없습니다.'를 반환한다. 현재 점수나 0점으로 채우지 않는다.

개인화 isLiked가 포함되므로 응답은 Cache-Control: no-store다.

## GET /api/spots/{spotId}/quiet-index/forecast

```text
GET /api/spots/42/quiet-index/forecast?date=2026-09-14&hour=0
```

지정한 시간부터 24시간 슬롯을 시간순으로 반환한다. spotId는 백엔드 숫자 ID다.

```json
{
  "timezone": "Asia/Seoul",
  "spotId": 42,
  "timeline": [
    {
      "type": "FORECAST",
      "targetAt": "2026-09-14T00:00:00+09:00",
      "quietIndex": null,
      "generatedAt": null,
      "validUntil": null,
      "source": null,
      "modelVersion": null
    }
  ]
}
```

예시는 첫 슬롯만 표시했으며 실제 응답은 항상 24개다. 누락된 시간의 점수는 null이다. 현재 점수, 인접 시간 점수, 전날 같은 시각으로 채우지 않는다.

같은 대상 시간에 대한 추천 목록과 타임라인은 동일한 예측 버전을 사용한다. 조회 사이에 새로운 예측이 도착했다면 응답이 달라질 수 있으며 generatedAt을 비교하면 된다.

## POST /api/internal/quiet-index/forecasts

**AI팀 협의용 신규 수신 계약**이다. 현재 AI의 /recommend, /quiet-index/map 응답 형식이 아니다.

인증은 기존 내부 API처럼 X-Internal-Api-Key 헤더를 사용한다. 값은 internal.api-key와 같아야 한다. AI를 호출할 때 쓰는 X-API-Key/AI_API_KEY와는 별개의 방향·설정이다.

```json
{
  "source": "coltrip-ai",
  "modelVersion": "forecast-v1",
  "generatedAt": "2026-09-13T12:00:00+09:00",
  "forecasts": [
    {
      "tourApiContentId": "126508",
      "targetAt": "2026-09-14T15:00:00+09:00",
      "quietIndex": 82.345,
      "validUntil": "2026-09-14T16:00:00+09:00"
    }
  ]
}
```

- source는 설정 forecast.source(기본 coltrip-ai)와 일치해야 한다.
- modelVersion은 AI가 관리하는 실제 모델 버전이다. 백엔드가 임의 버전을 생성하지 않는다.
- generatedAt은 모델이 예측을 생성한 시각이다. 백엔드 수신 시각과 다르며 미래 시각은 거부한다.
- receivedAt은 백엔드가 실제로 받은 시각을 내부 기록한다. generatedAt 대신 사용하지 않는다.
- targetAt은 예측 대상 시간 슬롯 시작이며 한국 시간 정시여야 한다.
- 대상 시간은 생성 시각의 시간 슬롯부터 7일 뒤 같은 슬롯까지다.
- validUntil은 AI가 정한 이 예측 버전의 유효기간이다. generatedAt 이후, 대상 슬롯 종료 이하만 허용한다. 만료된 데이터를 늦게 받아 저장할 수는 있지만 조회에는 노출하지 않는다.
- quietIndex는 0~100. 소수점 둘째 자리까지 HALF_UP 반올림해 DECIMAL(5,2)로 저장한다. 기존 정수 현재 점수는 변경하지 않는다.
- forecasts는 1~1000개. 같은 배치의 같은 장소/대상 시간 중복은 400.
- TourAPI 원본 contentId로 연결한다. 숫자 문자열만 허용하고 SEED-*/POI*를 날짜별 예측 ID로 임의 매핑하지 않는다.
- 백엔드에 없는 장소가 하나라도 있으면 404와 배치 전체 롤백. 관광지를 자동 생성하지 않는다.

동일 (spot_id, target_at, source, generated_at)은 정정으로 upsert한다. 새 generatedAt은 별도 버전으로 저장한다. 장소 행 잠금과 유니크 제약을 사용하며 여러 장소 배치는 일정한 장소 ID 순서로 잠근다.

```json
{"created": 1, "updated": 0}
```

이 응답은 저장 건수이며 실제 추천 노출 건수는 아니다.

### 최신 버전 선택

조회 시 해당 출처/장소/대상 시간에서 조회 시각 이하의 가장 최신 generatedAt을 선택한다. 선택한 버전이 만료되었다면 데이터 없음으로 처리한다. 과거 버전이 아직 유효하더라도 최신 버전 대신 되살리지 않는다.

## 오류

| 상황 | 응답 |
|---|---|
| 날짜/시간/limit/enum 형식 오류 | 400 |
| 예측 수신 필드/시간 관계/출처/중복 오류 | 400 |
| 타임라인 장소 없음, 수신 대상 장소 미등록 | 404 |
| 내부 인증키 없음/불일치/서버 키 미설정 | 401 |
| 유효한 예측 데이터 없음 | 200 + 빈 추천 목록 또는 null 타임라인 슬롯 |

기존 ErrorResponse의 code/message 형식을 따른다. 공개 조회에서 AI를 직접 호출하지 않기 때문에 AI 타임아웃을 조회 오류로 매번 전달하지 않는다. 새 예측이 오지 않으면 기존 예측도 validUntil까지만 노출한다.

## 스키마와 운영

quiet_forecast 테이블을 새로 사용한다. quiet_index 관측 이력이나 tourist_spot 현재 점수 컬럼은 변경하지 않는다.

| 컬럼 | 타입 | 의미 |
|---|---|---|
| id | BIGINT PK | 내부 ID |
| spot_id | BIGINT FK | tourist_spot |
| target_at | DATETIME(6) | 대상 슬롯 시작 |
| generated_at | DATETIME(6) | AI 생성 시각 |
| valid_until | DATETIME(6) | 유효기간 |
| received_at | DATETIME(6) | 백엔드 수신 시각 |
| quiet_index | DECIMAL(5,2) | 예측값 |
| source | VARCHAR(64) | 데이터 출처 |
| model_version | VARCHAR(100) | 모델 버전 |

유니크 키는 (spot_id, target_at, source, generated_at), 조회 인덱스는 (target_at, source, generated_at)이다.

현재 구현은 SQL에서 대상 시간/출처/category/mode 필터를 먼저 적용한 후 정렬을 Java에서 계산한다. 위치 필터가 없어졌으므로 필터 후 전체 후보를 읽기 때문에 관광지 규모가 크게 늘면 페이지 처리를 별도로 검토한다.

이력 보존 기간·오래된 예측 정리 배치·AI 재전송 주기는 이번 구현에 포함하지 않는다.

## 아직 필요한 AI팀 확인

1. 특정 날짜별 예측값을 만들 수 있는지, 모델의 실제 예측 가능 기간은 얼마인지.
2. generatedAt/targetAt/validUntil/source/modelVersion을 이 계약으로 제공할 수 있는지.
3. 실제 TourAPI contentId 기준 데이터와 전송 주기, 실패 시 재전송 정책.
4. 이 수신 API로 보낼지 또는 다른 예측 API/파일 계약을 사용할지.

현재 공개된 시간대별 추정 결과를 이 계약에 넣으려면 먼저 그것이 진짜 대상 날짜 예측인지 AI팀이 정의해야 한다. 그 정의 없이 미래 날짜와 생성 시각을 백엔드가 만들어 넣지 않는다.

## 운영 관례 — 현재 예측은 하루 1값(2026-09-16)

AI의 예측 모델은 실제로는 날짜 단위 값만 생성한다(시간대별 세분화 없음). `scripts/push_forecasts.py`는 이 계약에 맞추기 위해 매일 하나의 `targetAt`을 **15:00 KST로 고정**해서 하루 1건만 전송한다. 24시간 슬롯 API(`GET .../quiet-index/forecast`) 자체의 반환 개수(24개)는 바뀌지 않았고, 코드도 변경하지 않았다 — 단지 15시 슬롯만 실제 값이 채워지고 나머지 23개는 기존 null 처리 로직대로 비어 있을 뿐이다.

프론트는 날짜 단위 캘린더 뷰(월~일)를 만들 때 `hour=15`로 고정해서 조회한다. 다른 hour로 조회하면 정상적으로 빈 결과가 나온다 — 버그가 아니다. 모델이 실제 시간대별 예측을 제공하게 되면 이 관례는 제거한다.
