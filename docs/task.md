# 백엔드 구현 태스크

[api.md](./api.md), [schema.md](./schema.md) 기준 구현 체크리스트. 진행하면서 상태 갱신.

## 완료

- [x] GitHub 레포/CI/브랜치 전략 세팅
- [x] NCP Maps(Dynamic Map/Geocoding/Reverse Geocoding) Application 등록, Client ID/Secret 발급
- [x] Google OAuth Web 클라이언트 등록 (백엔드 토큰 검증용)
- [x] DB(MySQL), 인증 방식(구글+JWT) 결정
- [x] AI팀과 QuietIndex/감성모드/대체지 스키마 1차 협의
- [x] api.md, schema.md 초안 작성

## 블로커 — 2026-08-18 회의에서 해소됨 (문서 반영은 Phase 4/5 착수 시 일괄 진행 예정)

- [x] `category`(장소유형) enum 8종 확정 (기타 제외, 데이터 많으면 추가 확장 가능)
- [x] 대체지 추천: **실시간 호출**로 확정 (schema.md의 배치 저장 가정은 재설계 필요)
- [x] `similarityScore` 스케일: **0~1**로 확정
- [x] QuietIndex 배치 계산 주기: **1시간**으로 확정 (하락 트리거 기능이 무의미해지면 재논의)
- [x] AI↔백엔드 연동 방식: AI가 **API로 push** (DB 직접 쓰기 아님) — 수신용 엔드포인트 신규 설계 필요

## Phase 4/5 착수 전 문서 반영 필요 (미착수)

- [x] QuietIndex 수신 API 신설 (AI → 백엔드 push) — `POST /api/internal/quiet-index`, `X-Internal-Api-Key` 인증. `api.md` 반영 완료
- [ ] `GET /api/spots/{spotId}/alternatives` 실시간 호출 구조로 재설계, `spot_alternative` 테이블 용도 재검토
- [x] 최초 추천 검색 반경 15km — `api.md`에 프론트 가이드로 문서화 완료
- [ ] 대체지 검색 반경 3km 캡 — 9b(대체지 실시간 호출 재설계)에서 함께 반영
- [ ] 대체지 없을 때: 빈 배열 + 안내 멘트 (3km 확장은 추후)
- [x] `visit` 테이블 `start_quiet_score` 컬럼 추가 (고요지수 하락 트리거용)
- [ ] 고요지수 하락 트리거 로직: 절대(40점 미만) OR 상대(15점 이상 하락), 도착 체크포인트에서 평가, 비강제 제안 (9b 이후 진행)
- [x] 방문완료 반경: 카테고리별(점형/면적형) 적용 완료 (실측 검증은 #48 별도 진행). 체류시간 조건은 2026-09 제거
- [x] 혼잡/보통/고요 구간 임계값(100점 만점): 0~40 CROWDED, 41~70 NORMAL, 71~100 QUIET — `QuietLevel` enum, `TouristSpot.getQuietLevel()` 반영 완료

---

## Phase 1 — 프로젝트 스캐폴딩

- [ ] `backend/`에 Spring Boot 프로젝트 생성 (Gradle Groovy, Java 17)
- [ ] 패키지 구조 설계 (`domain`, `api`, `config`, `infra` 등)
- [ ] MySQL 연동 설정 (application-local.yml, application-secret.yml — 둘 다 `.gitignore` 처리됨)
- [ ] JPA/Hibernate 설정, `schema.md` 기준 엔티티 6종 작성
  - [ ] `User`
  - [ ] `TouristSpot`
  - [ ] `QuietIndex` (이력)
  - [ ] `SpotMode`
  - [ ] `SpotAlternative`
  - [ ] `Visit`
- [ ] 로컬 빌드/구동 확인, GitHub Actions CI 정상 통과 확인 (지금은 skip 처리되어 있음 — 실제로 도는지 확인)

## Phase 2 — 인증 ([api.md](./api.md) [인증]/[사용자] 섹션) — PR #5, 머지 대기

- [x] Spring Security 설정 (JWT 필터, stateless, 커스텀 401 EntryPoint로 일관된 에러 응답)
- [x] `POST /api/auth/google` — 구글 idToken 검증 → intent(LOGIN/SIGNUP)에 따라 기존 유저 로그인/신규 가입 분기 → JWT 발급
- [x] `POST /api/auth/refresh` — 리프레시 토큰 검증/재발급
- [x] `POST /api/auth/logout` — 리프레시 토큰 무효화
- [x] JWT 인증 필터 (Authorization 헤더 검증, SecurityContext 등록, access/refresh 타입 구분)
- [x] 예외 처리: `InvalidGoogleTokenException`, `InvalidRefreshTokenException`, `UnauthorizedException`, `UserNotRegisteredException`, `AlreadyRegisteredUserException`
- [x] `GET /api/users/me`, `PATCH /api/users/me` — 닉네임 조회/설정(로그인 직후 필수 온보딩 + 마이페이지 수정 공용), 중복 허용
- [x] `DELETE /api/users/me` — 회원 탈퇴(하드 삭제), 연관 Visit 이력 함께 삭제. end-to-end 테스트 완료
- [x] 로컬 MySQL 대상 부트업 테스트 완료

## Phase 3 — 관광지 데이터 적재 + 조회 ([api.md](./api.md) [관광지] 섹션)

**범위 변경 (2026-08-18 회의)**: TourAPI 수집 + 카테고리 매핑(30종 세분류 → 8종 확정 enum)은 **AI가 소유**. 백엔드는 결과를 받아 저장/조회하는 쪽만 담당.

- [ ] ⚠️ **AI → 백엔드 데이터 전달 방식 확인 필요** — QuietIndex처럼 `POST /api/internal/spots` 같은 push API로 받을지, AI가 직접 DB에 upsert하는지, 파일(CSV/JSON) 넘겨받아 백엔드가 적재하는지 미확정. 확인되는 대로 아래 항목 구체화
- [ ] (전달 방식 확인 후) `TouristSpot` 데이터 적재 로직 — AI가 이미 8종으로 분류한 카테고리 값 그대로 저장
- [ ] Naver Geocoding 연동: 필요 여부 재확인 (TourAPI 좌표를 AI 파이프라인에서 이미 정제해서 넘겨줄 수도 있음)
- [x] `GET /api/spots` — bounding box + category + mode 필터, 목록 조회 **(AI 적재와 무관하게 선구현 완료)**
- [x] `GET /api/spots/{spotId}` — 상세 조회
- [x] 예외 처리: `InvalidBoundingBoxException`, `SpotNotFoundException`
- [x] 개발용 시드 데이터 (`backend/seed/seed-spots.sql`) — 부산 관광지 12곳, `SEED-` 접두어로 실제 데이터와 구분

> **조회 API는 적재 방식과 독립적**이라 먼저 구현함. AI 적재 방식이 확정되어 실제 데이터가 들어와도 조회 API는 그대로 동작함. 시드 데이터는 `DELETE FROM tourist_spot WHERE tour_api_content_id LIKE 'SEED-%';`로 정리 가능.

## Phase 4 — QuietIndex 연동

- [ ] AI가 배치로 써주는 `quiet_index` 테이블 스키마 확정 (AI팀과 테이블/컬럼 형식 맞추기 — 같은 DB 공유인지, AI가 API로 백엔드에 밀어주는지 확인)
- [ ] `TouristSpot.current_quiet_score` / `quiet_score_updated_at` 캐시 갱신 로직 (신규 `quiet_index` insert 시 트리거 or 별도 배치)
- [ ] `/api/spots`, `/api/spots/{id}` 응답에 quietScore 필드 반영 확인

## Phase 5 — 대체지 추천 ([api.md](./api.md) [대체지 추천] 섹션)

- [ ] (블로커 해소 후) 배치 저장이면: AI가 쓴 `spot_alternative` 읽는 조회 로직만 구현
- [ ] (블로커 해소 후) 실시간 호출이면: AI 서버 API 클라이언트 구현, 타임아웃/장애 처리
- [ ] `GET /api/spots/{spotId}/alternatives` 구현
- [ ] 예외 처리: `SpotNotFoundException`, 대체지 없음 케이스 처리 방식 확정 후 반영

## Phase 6 — 방문 플로우 ([api.md](./api.md) [방문] 섹션) — 완료 (트리거/대체지 제안 제외)

- [x] `POST /api/visits/start` — 방문 세션 생성, 중복 방문 체크(`AlreadyOngoingVisitException`), `start_quiet_score` 스냅샷 저장
- [x] `PATCH /api/visits/{visitId}/complete` — 반경 조건 검증 로직
  - [x] 목적지 반경 계산 (Haversine, `GeoUtils`), 카테고리별 반경(`Category.getVisitRadiusMeters()`) — 점형 100m/면적형 250m 잠정값
  - [x] ~~체류시간 10분(600초) 검증~~ → **2026-09 제거, 반경 진입만으로 판정 (팀 확정)**. 프론트 표시용으로 `visitRadiusMeters`를 장소 상세/현재 방문 조회 응답에 추가
- [x] 예외 처리: `VisitNotFoundException`, `InvalidVisitStateException`, `VisitConditionNotMetException`
- [x] 로컬 MySQL 대상 end-to-end 테스트 완료 (방문 시작→완료, 조건 미충족 케이스 포함)
- [x] `PATCH /api/visits/{visitId}/cancel` — 진행 중 방문 취소. 대체지 선택 등 목적지 전환 시 재사용, 취소 후 재시작/현재 방문 제외/방문 횟수 미포함 확인 완료
- [ ] 고요지수 하락 트리거 + 대체지 제안(비강제)은 별도 항목(2, 5번) — 9b 완료 후 진행

## Phase 7 — 좋아요 / 리뷰 (2026-08-31 추가) — 완료

- [x] `/api/spots` GET 인증 해제 (지도 둘러보기는 비로그인 허용, 쓰기는 인증 유지)
- [x] `GET /api/spots` 응답에 `address`, `imageUrl` 추가 (목록 카드 UI용)
- [x] 좋아요: `POST`/`DELETE /api/spots/{id}/like`, `GET /api/users/me/likes` — 멱등 처리
- [x] 리뷰: `POST /api/visits/{visitId}/review`, `GET /api/spots/{spotId}/reviews`, `DELETE /api/reviews/{reviewId}`
  - [x] ~~별점 대신 고요함 피드백(`QuietFeedback` 3단계)~~ → **별점(1~5) + 한줄평으로 변경 (2026-09-01)**
  - [x] 방문 완료자만 작성 가능, 방문 1건당 리뷰 1건(visit_id unique)
- [x] 회원 탈퇴 시 review/spot_like까지 연쇄 삭제 (FK 순서 주의)
- [x] 로컬 MySQL end-to-end 테스트 완료

## Phase 8 — 사용자 통계 / 리뷰 방식 변경 (2026-09-01) — 완료

- [x] `UserResponseDTO`에 `visitCount`(완료한 방문 수), `likeCount` 추가
  - 로그인·재발급·내 정보 조회·닉네임 수정 응답 전부 동일한 모양 유지
  - `UserStatsReader`로 집계 기준을 한 곳에 모아 로그인과 조회가 어긋나지 않게 함
- [x] 리뷰를 고요함 피드백 3단계 → **별점(1~5) + 한줄평**으로 변경
  - `QuietFeedback` enum 삭제, `Review.rating` 추가
  - 기존 `quiet_feedback` 컬럼은 `ddl-auto: update`가 삭제하지 않으므로 수동 DROP 필요
    (`ALTER TABLE review DROP COLUMN quiet_feedback;`)
- [x] 로컬 MySQL end-to-end 테스트 완료

---

## 우선순위 제안

1. Phase 1 (스캐폴딩) — 다른 모든 작업의 전제
2. Phase 2 (인증) + Phase 3 (관광지 조회) — AI 의존 없음, 병행 가능
3. 블로커 해소 (AI 협의) — Phase 4, 5 착수 전 필수
4. Phase 4 (QuietIndex 연동)
5. Phase 5 (대체지)
6. Phase 6 (방문 플로우) — 다른 Phase와 독립적이라 언제든 병행 가능
