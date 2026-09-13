# 관광지 기본정보 및 감성모드 적재

## 계약 상태

관련 이슈: #45 (#23은 중복 이슈로 종료).
백엔드 수신 API를 구현한 상태이며 AI팀의 전송 담당, push 방식 및 아래 필드 계약은 협의가 필요하다.
TourAPI 수집·정제·카테고리 분류는 AI 담당이다. 이 기능이 AI의 전송 작업을 대신하지 않는다.

## 요청

`POST /api/internal/spots`

- `Content-Type: application/json`
- `X-Internal-Api-Key`: 기존 내부 API 키 설정 재사용. 프론트에 공개하지 않는다.
- 서버 간 HTTPS로 호출한다. 사용자 JWT가 아닌 내부 키로 인증한다.

다음은 형식 설명용 예시이며 실제 관광지 데이터가 아니다.

```json
{
  "spots": [{
    "tourApiContentId": "123456",
    "name": "예시 관광지",
    "address": "부산광역시 예시 주소",
    "latitude": 35.1796,
    "longitude": 129.0756,
    "category": "PARK",
    "description": null,
    "imageUrl": null,
    "recommendReason": "산책하기 좋은 장소",
    "modes": ["WALK", "SCENERY"],
    "sourceUpdatedAt": "2026-09-12T12:00:00+09:00"
  }]
}
```

- spots: 1~100개. 한 배치에서 동일 contentId 중복은 오류.
- tourApiContentId: 실제 TourAPI contentId 숫자 문자열(1~50자). SEED/POI 식별자 불허.
- name/address: 필수, 각각 최대 200/300자.
- latitude/longitude: 필수, 각각 -90~90/-180~180, 소수점 최대 7자리.
- category: CAFE, PARK, LIBRARY, GALLERY, BOOKSTORE, TEMPLE, BEACH, ALLEY.
- modes: 필수 목록, 최대 20개. WALK, CONTEMPLATION, SCENERY, WATER_GAZING, CULTURE.
- description/imageUrl/recommendReason: 선택, 각각 최대 10000/500/500자. 누락/null이면 기존 값을 지운다.
- imageUrl: HTTP(S) 및 정상 호스트, 사용자 인증정보 없는 주소만 허용. 서버에서 이미지를 다운로드하지 않는다.
- sourceUpdatedAt: 필수 오프셋 포함 시각. AI 정제 데이터의 갱신 시각이며 재전송 시 임의 변경하지 않는다. 미래 시각 불허.

## 저장 정책

전체 스냅샷 방식이며 PATCH가 아니다. modes 중복은 제거하고 빠진 모드는 해제한다.
빈 배열은 모드 전부 해제, null/누락은 오류이다. 유지되는 모드 매핑 ID는 보존한다.
같은 tourApiContentId는 같은 관광지 ID로 갱신한다. 방문·좋아요·리뷰 참조와 고요지수/이력/예측은 변경하지 않는다.
MySQL 유니크 키로 행을 확보한 뒤 기존 고요지수 push와 같은 행 잠금을 사용한다. 삭제 후 재생성하지 않는다.
잠금과 응답 순서는 contentId 정렬 순서다. DB 오류 시 배치 전체 롤백하며 장애 해결 후 동일 배치 재전송이 가능하다.

sourceUpdatedAt은 한국 시간, 마이크로초 단위로 저장한다. 저장값보다 과거이면 IGNORED_STALE로 무시한다.
같거나 최신이면 정정 적용한다. 기존 저장값이 null인 장소는 최초 수신을 적용한다.

## 응답

```json
{
  "applied": 1,
  "ignoredStale": 0,
  "spots": [{
    "tourApiContentId": "123456",
    "spotId": 101,
    "status": "APPLIED",
    "sourceUpdatedAt": "2026-09-12T12:00:00+09:00"
  }]
}
```

spotId는 실제 생성된 백엔드 ID이다. APPLIED는 생성/수정/동일 재전송을 모두 포함하며 신규 생성 수가 아니다.
IGNORED_STALE 항목의 sourceUpdatedAt은 DB에 유지된 최신 시각이다.
유효한 본문 기준 키 누락/오류는 401, 입력 오류는 400이다. 본문 검증은 키 인증 메서드보다 먼저 실행될 수 있다.
DB 장애를 정상 성공으로 반환하지 않는다.

## DB 변경 및 연결

신규 컬럼: tourist_spot.source_updated_at (DATETIME(6), nullable).
대상 DB에서 존재 여부를 확인하고 팀의 마이그레이션 절차에 따라 적용한다. 개발 ddl-auto:update는 자동 추가할 수 있다.

```sql
SHOW COLUMNS FROM tourist_spot LIKE 'source_updated_at';
-- 컬럼이 없는 경우에만 적용한다.
ALTER TABLE tourist_spot ADD COLUMN source_updated_at DATETIME(6) NULL;
```

기본정보 API 성공 응답 후 동일 tourApiContentId로 기존 POST /api/internal/quiet-index를 호출한다.
기본정보 커밋 전 점수를 보내면 미등록 장소 오류가 날 수 있다.
적재 결과는 기존 GET /api/spots 및 GET /api/spots/{spotId}로 조회한다.

시드는 자동 삭제하지 않는다. 실제 DB의 시드 잔존 여부는 별도 확인해야 한다.
시드 정리 전 visit, spot_like, review, quiet_index, quiet_forecast, spot_mode,
spot_alternative의 양쪽 참조 및 visit.alternative_suggestion_json 내부 장소 ID를 확인한다.
실 contentId 적재만으로 SEED 참조가 이전되지 않는다. 백업과 별도 이전 정책 없이 삭제하거나 FK 검사를 끄지 않는다.

## 검증

backend 폴더에서 다음 명령으로 실행한다.

```powershell
.\gradlew.bat test --tests "com.coltrip.backend.internal.spot.*"
```

컨트롤러 5개 및 H2(MySQL 모드) 통합 테스트 6개를 포함한다.
실제 MySQL 동시 요청, AI 실데이터 전송, 배포 환경 보안 필터 전체 검증은 별도 수행해야 한다.
