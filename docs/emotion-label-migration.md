# 감성 라벨 8종 전환

API 필드명 mode/modes는 유지하고 값만 변경한다.

| API/DB 값 | 의미 |
|---|---|
| COZY | 아늑 |
| NATURAL | 자연 |
| URBAN | 도시 |
| VINTAGE | 빈티지 |
| EXOTIC | 이국 |
| VIBRANT | 활기 |
| SENSORY | 감각 |
| TRANQUIL | 고요 |

Mode.getLabel()은 한국어 설명용이며 JSON과 DB는 enum 이름을 사용한다.
GET /api/spots와 /api/spots/recommendations의 mode 필터 및 내부 적재 modes 목록에 적용한다.
기존 WALK/CONTEMPLATION/SCENERY/WATER_GAZING/CULTURE는 더 이상 허용하지 않는다.
구 라벨이나 알 수 없는 값은 쿼리에서 400 InvalidParameterException, 적재 JSON에서 400 InvalidRequestBodyException이다.
TRANQUIL은 감성 분류이며 실시간 QuietLevel.QUIET 또는 특정 고요지수 구간과 동일한 의미가 아니다.
카테고리, 반경, 고요지수 계산 정책은 변경하지 않는다.

## 호환성과 배포 순서

활동 유형에서 분위기 분류로 바뀌므로 의미가 보장되는 일대일 변환은 없다.
코드만 배포하면 기존 spot_mode의 구 enum을 JPA가 읽을 때 실패할 수 있다.
ddl-auto:update가 기존 데이터 값을 올바른 감성으로 재분류하지는 않는다.

1. 프론트 필터/표시 상수와 AI 정제 데이터의 modes를 새 8종으로 맞춘다.
2. 실제 DB의 spot_mode 및 visit.alternative_suggestion_json을 백업하고 영향 행을 확인한다.
3. 점검 시간 동안 서버와 AI 쓰기를 중지한다. SHOW CREATE TABLE spot_mode로 VARCHAR/ENUM/CHECK 제약을 확인한다.
4. 검증한 신규 분류 데이터가 있으면 장소별 매핑을 교체한다. 구→신 자동 치환은 하지 않는다.
5. 재분류가 아직 없고 잠시 감성 미분류를 허용하기로 팀이 승인했다면, 백업 후 구 라벨 매핑만 제거하여 modes=[]로 두는 전환을 선택할 수 있다. 관광지/방문/좋아요/리뷰는 삭제하지 않는다.
6. DB enum 또는 CHECK 제약이 있다면 새 값 8종을 허용하도록 별도 마이그레이션한다. 실제 DDL을 확인하지 않고 ALTER를 실행하지 않는다.
7. 진행 중 제안 JSON은 옛 modes를 포함할 수 있다. 별도 백업 후 해당 방문의 제안을 만료/닫음 처리하거나 검증된 데이터로 갱신한다. JSON 전체를 문자열 치환하지 않는다.
8. 구 라벨이 없는 것을 확인하고 새 서버를 시작한다. AI가 sourceUpdatedAt을 갱신한 스냅샷을 재전송해 감성모드를 채운다.

점검용 SQL(읽기 전용):

```sql
SELECT mode, COUNT(*) FROM spot_mode GROUP BY mode;
SHOW CREATE TABLE spot_mode;
SELECT sm.id, sm.spot_id, sm.mode, s.tour_api_content_id
FROM spot_mode sm JOIN tourist_spot s ON s.id = sm.spot_id
WHERE sm.mode NOT IN ('COZY','NATURAL','URBAN','VINTAGE','EXOTIC','VIBRANT','SENSORY','TRANQUIL');
SELECT id, status, alternative_suggestion_json FROM visit
WHERE alternative_suggestion_json IS NOT NULL;
```

이 작업에서 실제 DB 조회·변경·삭제는 실행하지 않았다. 기존 DB가 있다면 위 전환을 완료해야 한다.
seed-spots.sql은 신규 개발 DB용 예시 분류이고 실제 AI 분류가 아니다. 기존 DB에 전체 시드를 재실행하는 방법으로 전환하지 않는다.
테스트 픽스처의 라벨 교체도 운영 데이터 변환 규칙을 뜻하지 않는다.

## 운영 반영 완료 (2026-09-13)

코드 배포가 위 전환 없이 먼저 나가면서 `/api/spots`, `/api/spots/{id}`가 기존 `spot_mode`의 구 라벨(WALK/CONTEMPLATION/SCENERY/WATER_GAZING/CULTURE)을 읽다가 500이 발생했고, 이 예외가 `/error`로 전달되며 401로 잘못 표시되는 일이 실제로 발생했다(원인 확인 후 조치).

5번 방식으로 전환 완료: 백업 후 `spot_mode` 11개 행(전부 구 라벨, 새 8종 검증 데이터 없었음) 전체 삭제, 현재 모든 장소 `modes=[]`. `tourist_spot`/`visit`/`spot_like`/`review`는 변경 없음. `visit.alternative_suggestion_json`에 걸린 진행 중 제안 없음(7번 해당 없음). 정상화 확인: 목록/상세/모드 필터/추천/대체지 API 200 응답.

남은 것: AI가 594개 관광지를 새 8종 라벨로 분류해 `POST /api/internal/spots`로 재전송하면 그때 `modes`가 채워짐(8번).
