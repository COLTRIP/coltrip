## 방문 중 고요지수 하락 및 대체지 제안

### 평가 시점과 범위

기존 도착 시점 단독 평가를 방문 중 평가로 확장하는 제안안이다. 프론트는 방문 화면 진입/복귀 시, 그리고 STARTED 방문을 표시하는 동안 60초 간격으로 이전 요청이 끝난 뒤 평가 API를 호출한다. 도착 직전에도 호출할 수 있지만, 평가 성공을 기다려야 완료할 수 있는 흐름으로 만들지 않는다.

완료 API는 현재처럼 반경을 검증하고 완료 처리만 한다. 완료 응답에 추천을 끼워 넣지 않는다. 완료 이후 check는 INACTIVE를 반환한다. AI 실패로 check가 실패해도 완료 API는 독립적으로 호출할 수 있다.

이번 구현은 앱이 열려 있을 때의 폴링 방식이다. 백그라운드 푸시, OS 알림, SSE, 서버 스케줄러는 포함하지 않는다. 이 정책은 기존 문서의 도착 시점 단독 평가와 다르므로 프론트/팀 합의 후 반영한다.

### 점수 정책

- 현재 점수는 tourist_spot.current_quiet_score와 quiet_score_updated_at으로 평가한다.
- 현재 점수 < 40 또는 유효한 시작 점수 - 현재 점수 >= 15이면 제안 조건 충족이다.
- 40 자체는 절대 조건을 충족하지 않는다. 예를 들어 시작 55/현재 40은 상대 조건으로 충족한다.
- 현재 점수가 null/범위 밖이면 SCORE_UNAVAILABLE이다. 0으로 대체하지 않는다.
- 현재 점수 시각이 null, 미래이거나 5시간 초과로 오래되었으면 SCORE_STALE이다. 정확히 5시간이면 유효하다.
- 시작 점수는 방문 시작 당시 기록한 값과 그 관측 시각을 사용한다. 시작 당시 관측값이 5시간 이내로 유효했어야 상대 하락 조건에 사용한다. 방문이 오래됐다고 시작 기준값 자체를 현재 시각 기준으로 폐기하지 않는다.
- 시작 점수나 관측 시각이 없으면 절대 조건만 평가한다. 현재 39는 제안 가능, 현재 40 이상은 BASELINE_UNAVAILABLE이다.
- 기존 방문의 새 start_quiet_score_observed_at 컬럼은 null로 유지한다. 과거 시각을 임의로 채우지 않는다.
- 5시간은 현 상황의 4시간 데이터 갱신 간격을 고려한 임시 정책이지 팀 확정값이 아니다. 실시간 측정이라는 의미도 아니다. 설정으로 변경한다.
- 서버 JVM과 DB에 기록하는 LocalDateTime 기준은 Asia/Seoul로 통일해야 한다. 기존 데이터가 UTC라면 단순 시간대 설정 변경 전에 데이터를 점검한다.

### POST /api/visits/{visitId}/alternative-suggestion/check

인증 필수. 본인의 방문만 조회한다. 요청 바디 없음.

신규 제안 저장이 가능한 평가이므로 GET 대신 POST를 사용한다.

정상 제안 응답의 구조는 다음과 같다. 장소/점수는 형식 설명용 예시다.

```json
{
  "state": "OFFERED",
  "proposal": {
    "suggestionId": 10,
    "sourceSpotId": 2,
    "startQuietScore": 70,
    "currentQuietScore": 30,
    "sourceObservedAt": "2026-09-13T12:00:00",
    "issuedAt": "2026-09-13T12:01:00",
    "expiresAt": "2026-09-13T12:11:00",
    "alternatives": [
      {
        "spot": {
          "id": 3,
          "name": "예시 대체 장소",
          "address": "부산광역시",
          "category": "PARK",
          "modes": [],
          "imageUrl": null,
          "latitude": 35.01,
          "longitude": 129.0,
          "isLiked": false
        },
        "quietIndex": 70.5,
        "distanceKm": 1.1,
        "score": 0.8,
        "recommendReason": "주변의 더 한적한 장소입니다."
      }
    ]
  }
}
```

proposal이 없으면 다음과 같이 반환한다.

```json
{
  "state": "NO_ALTERNATIVES",
  "proposal": null
}
```

| state | 의미/프론트 처리 |
|---|---|
| OFFERED | 제안 있음. 같은 suggestionId는 팝업을 중복 표시하지 않고 기존 제안을 유지 |
| NOT_TRIGGERED | 현재 유효한 점수가 하락 조건에 해당하지 않음 |
| BASELINE_UNAVAILABLE | 절대 조건은 미충족이며 상대 비교 기준값이 없음/유효하지 않음 |
| SCORE_UNAVAILABLE | 현재 점수 없음/범위 오류. 임의 0점 표시 금지 |
| SCORE_STALE | 현재 관측 시각 없음/미래/오래됨. 다음 유효 데이터까지 대기 |
| DISABLED | 사용자가 알림 허용을 꺼 둠. 제안 숨기기 |
| DISMISSED | 해당 방문에서 제안을 닫음. 다시 제안하지 않음 |
| INACTIVE | 이미 완료/취소된 방문. 폴링 종료 |
| NO_ALTERNATIVES | AI 조회 성공 후 표시 가능한 후보가 없음. '주변 대체 장소가 없습니다' 표시 |
| AI_NOT_TRIGGERED | 백엔드 관측 점수는 조건 충족했지만 AI 자체 판정은 미충족. AI/DB 시점이 다를 수 있음 |
| SCORE_CHANGED | AI 대기 중 DB 점수/관측 시각 변경. 이번 결과 버리고 다음 평가에서 재시도 |
| EXPIRED | 이미 생성된 제안의 시간 만료 또는 점수 변경/조건 해제. 제안 숨기기, 이 방문에는 재생성하지 않음 |

OFFERED 이외에는 proposal=null이다. startQuietScore는 저장된 시작 점수이며 시각 검증 실패 시 상대 판정에는 쓰지 않는다. currentQuietScore는 백엔드가 판정에 사용한 관측값이고, 후보 quietIndex는 AI 응답값이다. AI 값과 백엔드 관측값을 같은 시점의 측정이라고 취급하지 않는다.

추천 호출은 기존 AlternativeService를 재사용한다. 원래 장소 기준 3km 필터, 중복/미등록 장소 제외, 최대 3개와 score(거리 감점 포함 추천 점수)를 그대로 따른다. 기존 공개 GET /api/spots/{spotId}/alternatives는 수동 조회용으로 유지되며 알림 거부 설정으로 차단하지 않는다.

제안의 장소 정보/좋아요/점수는 발행 당시 스냅샷이다. 목록 카드에 즉시 반영할 좋아요 값이 필요하면 기존 좋아요 API 응답으로 프론트 상태를 갱신한다. 이 스냅샷을 최신 고요지수 캐시로 사용하지 않는다.

### 중복 제안 및 만료

방문당 하나의 proposal JSON을 DB에 저장한다. suggestionId는 visitId와 같으며 재조회/동시 요청은 같은 제안을 반환한다. 응답을 못 받은 경우 재조회해 복원할 수 있도록 단순 '전송 완료' boolean으로 소비하지 않는다.

프론트는 suggestionId 기준으로 이미 보여준 제안을 중복 팝업으로 띄우지 않는다. 서버는 한 번 생성된 제안을 재생성하지 않으며, 사용자가 닫거나 10분 만료/원래 장소 점수 변경으로 무효화되면 같은 방문에서 추가 제안하지 않는 보수적인 정책이다. 재알림이 필요하면 별도 회차/쿨다운 정책을 정해야 한다.

사용자 행 잠금으로 한 방문에 서로 다른 제안이 저장되거나 선택으로 STARTED 방문이 중복 생성되는 것을 방지한다. AI 호출 자체는 잠금 밖이므로 동시에 시작된 요청이 각각 AI를 호출할 가능성은 있다. 외부 호출의 정확히 한 번 실행까지 보장하는 코드는 아니다.

### POST /api/visits/{visitId}/alternative-suggestion/dismiss

인증 필수, 바디 없음. 해당 방문의 제안을 닫고 이후 제안을 차단한다. AI 요청이 진행 중이어도 저장 시 닫힘 상태를 재확인한다.

```json
{"state": "DISMISSED", "proposal": null}
```

이미 완료/취소된 방문은 409. 같은 진행 중 방문에서 재요청해도 DISMISSED다.

### POST /api/visits/{visitId}/alternative-suggestion/select

인증 필수. 사용자가 제안된 장소를 직접 선택했을 때 호출한다. 자동 이동시키지 않는다.

```json
{
  "spotId": 3,
  "startLatitude": 35.01,
  "startLongitude": 129.0
}
```

응답은 기존 VisitStartResponse와 같다.

```json
{
  "visitId": 11,
  "status": "STARTED",
  "startedAt": "2026-09-13T12:02:00"
}
```

- 기존 방문 10은 CANCELED, 새 방문 11은 STARTED.
- 취소와 시작은 같은 DB 트랜잭션. 새 장소 없음/새 방문 시작 실패라면 취소도 롤백하도록 구성.
- 프론트가 기존 cancel과 start를 별도로 호출하지 않는다.
- 유효한 제안 목록에 없는 장소, 만료, 닫힘, 알림 거부 상태는 409.
- 기존 방문에서 선택이 이미 완료됐으면 동일 목적지 재요청은 같은 새 방문을 반환. 새 방문이 이후 완료됐다면 그 현재 상태를 반환하므로 STARTED라고 고정해서 해석하지 않는다.
- 동일 요청 경로로 다른 장소를 재선택하면 409.
- 프론트는 반환받은 visitId로 방문 화면/currentVisitId 상태를 교체한다.
- 응답의 시간 필드는 한국 로컬 시각이다.

### 오류와 방문 완료 독립성

본인 아닌 방문/없는 방문은 404, 인증 안 됨은 기존 보안 설정의 401이다.

AI 주소/키 누락은 503, 타임아웃은 504, 연결/응답 오류는 502, 기준점 AI 매핑 불가는 409다. 기존 {"code":"...","message":"..."} 오류 형식을 사용한다. AI 실패를 NO_ALTERNATIVES로 숨기지 않는다. 제안을 저장하지 않았으므로 다음 폴링에서 재시도 가능하다.

방문 완료/취소 서비스에서는 AI를 호출하지 않는다. 프론트도 check 실패를 이유로 완료 버튼을 막거나, check가 성공해야만 complete를 호출하도록 연결하면 안 된다.

### 데이터 갱신 선행 조건

이 코드는 고요지수를 직접 수집하거나 1시간/4시간마다 갱신하지 않는다. 기존 POST /api/internal/quiet-index로 관측 점수와 실제 계산 시각이 공급돼야 한다. AI가 배포돼 있다는 것만으로 백엔드 캐시가 자동 갱신되는 것은 아니다.

AI와 백엔드 장소 ID 매핑, AI 인증 설정, 최신 점수 공급이 준비되지 않으면 제안을 제대로 시연할 수 없다. AI 응답에는 관측 시각이 없어 응답 수신 시각을 임의 측정 시각으로 만들어 DB를 덮어쓰지 않는다.

### 스키마

visit 테이블에 아래 nullable 컬럼 4개를 추가한다.

| 컬럼 | 타입 | 용도 |
|---|---|---|
| start_quiet_score_observed_at | DATETIME(6) NULL | 시작 점수의 실제 관측 시각 스냅샷 |
| alternative_suggestion_json | LONGTEXT NULL | 방문별 단일 제안 및 후보 목록 스냅샷 |
| alternative_dismissed_at | DATETIME(6) NULL | 해당 방문에서 사용자가 제안을 닫은 시각 |
| alternative_selected_visit_id | BIGINT NULL | 선택으로 만들어진 방문 ID. 재시도 중복 생성 방지 |

새 추천 테이블은 만들지 않는다. spot_alternative에는 저장하지 않는다. 새 컬럼은 기존 방문에 null로 두며 데이터 삭제가 없다.

이슈 완료 전에는 실제 MySQL에서 동시 완료/취소/선택, 선택 실패 롤백, 멀티 인스턴스 중복 요청과 프론트 흐름을 별도로 검증해야 한다.
