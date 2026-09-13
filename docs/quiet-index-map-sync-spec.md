# AI 고요지수 지도 API 풀링 연동

관련 이슈: #63. 기존 [내부/AI 연동] 섹션의 push 방식(`POST /api/internal/quiet-index`, 이슈 #36)과는 **반대 방향**이다 — 여기는 백엔드가 AI를 호출한다.

## AI 서버 계약 (2026-09-13 AI 코드 대조, 이슈 #82)

```
GET https://ai.coltrip.co.kr/quiet-index/map?hour={0-23}&is_weekend={true|false}
X-API-Key: {AI_API_KEY}
```

응답은 `QuietIndexMapItem` 배열 (전체 POI):

```json
[{"poiId": "126081", "name": "해운대해수욕장", "lat": 35.1587, "lng": 129.1604, "quietIndex": 85.4}]
```

- 백엔드는 **KST** 기준 `hour`와 주말 여부를 계산해 `is_weekend`로 전송한다.
- AI GET 함수의 쿼리 이름은 `is_weekend`이다. `isWeekend`로 보내면 주말 인자가 기본값 `false`로 처리될 수 있다. 다른 AI POST API의 JSON `isWeekend` 필드와 구분한다.
- JSON 응답 필드(`poiId`, `quietIndex`)는 camelCase를 유지한다. 관련 코드: AI `app/api/routes_quiet_index.py`, `app/schemas/schemas.py`.
- 키 누락 422, 키 불일치 401, 존재하지 않는 poiId 요청 시 500 가능(우리는 이 엔드포인트를 poiId 지정 없이 전체 조회로만 쓰므로 해당 없음).
- **AI팀 요청사항**: 전체 POI를 순회해 계산하므로 호출 빈도를 제한적으로 유지할 것. SKT 실시간 소스(18곳)는 4시간 캐시 + 09~21시만 호출되고, 그 외 시간엔 자동으로 폴백 값이 내려온다.

## 백엔드 동기화 방식

- `AiQuietIndexMapClient.fetchMap(hour, isWeekend)` — `ai.base-url`/`ai.api-key`(대체지 추천과 설정 공유)로 GET 호출, 인증/설정 누락·타임아웃·5xx를 구분해 예외 처리
- `QuietIndexMapSyncScheduler` — 매시 5분(`quiet-index.map-sync.cron`, 기본 `0 5 * * * *`)에 KST 기준 hour/주말 여부를 계산해 1회 호출. **지도 요청마다 호출하지 않고 이 스케줄러만 AI를 부른다**
- 우리 DB에 있는 `tourApiContentId`만 응답에서 골라 반영(594개 중 대부분은 우리에게 없는 장소이므로 무시)
- `QuietIndexMapApplier`가 매칭된 장소 1건씩 잠금 후 `QuietIndexCacheWriter`(push 경로와 공유)로 이력 upsert + `current_quiet_score` 캐시 갱신
- AI 호출 자체가 실패(타임아웃/5xx/미설정)하면 이번 주기는 건너뛰고 **마지막 정상값을 그대로 유지**(현재 점수를 지우거나 0으로 채우지 않음)
- 우리 DB에 등록된 장소가 하나도 없으면(seed 미적재 등) AI를 호출하지 않는다

## 점수 스케일 결정

- AI는 `quietIndex`를 float(0~100, 소수점 있음)로 내려준다. 기존 `tourist_spot.current_quiet_score`/`quiet_index.quiet_score`는 Integer 스키마(이슈 #36에서 확정)이므로 **반올림해서 저장**한다. `QuietLevel`(혼잡/보통/고요) 임계값과 그대로 호환된다
- 예측 전용 테이블(`quiet_forecast`, 이슈 #44)은 이 결정과 별개로 소수점을 그대로 유지한다 — 관측/캐시와 예측은 저장 정밀도가 다르다

## 반영하지 않는 필드

- `lat`/`lng` — 지도 AI 응답의 좌표로 기존 관광지 좌표를 덮어쓰지 않는다. 현재 지도 응답에는 `population`이 없으며, 클라이언트의 nullable 필드는 저장·응답에 사용하지 않는다.
- `name` — 이미 우리 DB에 있는 이름을 신뢰하고 AI 응답의 name으로 덮어쓰지 않음(관광지 기본정보 갱신은 `POST /api/internal/spots`, 이슈 #45의 책임)

## 남은 것

- 운영 반영 후 첫 주기 로그(`AI 고요지수 지도 동기화 완료: 응답 N건 중 M건 반영`)로 매칭률 확인
- AI가 594개 POI를 다 계산하는 데 걸리는 시간이 매시 5분 내에 안정적으로 끝나는지 운영 중 확인 필요(초과하면 cron 오프셋 조정)
