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

## 응답 검증 및 실패 처리 (#85)

- `quietIndex`는 null이 아닌 유한수이며 0~100이어야 한다. 반올림 전에 검증한다. 0과 100은 유효하고 음수/100 초과/NaN/무한대는 제외한다.
- null 항목, 없거나 공백뿐인 poiId, 미등록 장소는 제외한다. 식별자를 임의 변환하거나 신규 장소를 생성하지 않는다.
- 동일 poiId가 두 번 이상 나오면 해당 ID의 모든 항목을 제외한다. 값이 같아도 동일하며 첫 값/마지막 값을 임의로 선택하지 않는다.
- 제외된 장소의 이력·현재 점수·갱신 시각은 변경하지 않는다. 다음 정상 응답에서 다시 반영할 수 있다.
- 각 장소는 기존 별도 빈의 독립적인 트랜잭션으로 저장한다. 건별 저장 실패는 롤백 후 집계하고 다음 장소를 계속 처리한다. 동기화 루프 전체를 하나의 트랜잭션으로 묶지 않는다.
- 정상 빈 배열 `[]`는 응답 0건이다. 본문 없음/null, 배열이 아닌 본문, 해석 불가능한 JSON/필드 타입은 조회 전체 실패로 처리한다. 파싱 자체가 실패하면 건별 복구하지 않고 기존 값을 유지한다.
- 완료 로그는 `응답 N건, 성공 S건, 제외 K건, 실패 F건`이며 N=S+K+F이다. 중복은 ID 수가 아니라 제외된 항목 수로 집계한다. 조회 이후 삭제된 장소는 제외 건수에 포함한다.
- 실패 로그에 원본 응답, 키 또는 예외 메시지를 출력하지 않고 예외 클래스만 기록한다. 자동 즉시 재시도는 하지 않으며 다음 정기 동기화에서 다시 시도한다.
- 성공 건수는 이력 upsert 처리가 완료된 건수이다. 더 최신 캐시가 이미 있다면 기존 최신값 유지 정책에 따라 캐시가 바뀌지 않을 수 있다.

## 점수 스케일 결정

- AI는 `quietIndex`를 float(0~100, 소수점 있음)로 내려준다. 기존 `tourist_spot.current_quiet_score`/`quiet_index.quiet_score`는 Integer 스키마(이슈 #36에서 확정)이므로 **반올림해서 저장**한다. `QuietLevel`(혼잡/보통/고요) 임계값과 그대로 호환된다
- 예측 전용 테이블(`quiet_forecast`, 이슈 #44)은 이 결정과 별개로 소수점을 그대로 유지한다 — 관측/캐시와 예측은 저장 정밀도가 다르다

## 반영하지 않는 필드

- `lat`/`lng` — 지도 AI 응답의 좌표로 기존 관광지 좌표를 덮어쓰지 않는다. 현재 지도 응답에는 `population`이 없으며, 클라이언트의 nullable 필드는 저장·응답에 사용하지 않는다.
- `name` — 이미 우리 DB에 있는 이름을 신뢰하고 AI 응답의 name으로 덮어쓰지 않음(관광지 기본정보 갱신은 `POST /api/internal/spots`, 이슈 #45의 책임)

## 남은 것

- 운영 반영 후 첫 주기의 성공/제외/실패 로그로 매칭률 확인
- AI가 594개 POI를 다 계산하는 데 걸리는 시간이 매시 5분 내에 안정적으로 끝나는지 운영 중 확인 필요(초과하면 cron 오프셋 조정)
