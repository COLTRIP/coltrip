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

- [ ] QuietIndex 수신 API 신설 (AI → 백엔드 push), `api.md`에 추가
- [ ] `GET /api/spots/{spotId}/alternatives` 실시간 호출 구조로 재설계, `spot_alternative` 테이블 용도 재검토
- [ ] 대체지 검색 반경 3km 캡, 최초 추천 검색 반경 15km 반영
- [ ] 대체지 없을 때: 빈 배열 + 안내 멘트 (3km 확장은 추후)
- [ ] `visit` 테이블 `start_quiet_score` 컬럼 추가 (고요지수 하락 트리거용)
- [ ] 고요지수 하락 트리거 로직: 절대(40점 미만) OR 상대(15점 이상 하락), 도착 체크포인트에서 평가, 비강제 제안
- [ ] 방문완료 반경: 카테고리별(점형/면적형) 유동 적용, 체류시간 10분
- [ ] 혼잡/보통/고요 구간 임계값(100점 만점) — 팀 확인 대기 중

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
- [x] `POST /api/auth/google` — 구글 idToken 검증 → User 조회/생성 → JWT 발급
- [x] `POST /api/auth/refresh` — 리프레시 토큰 검증/재발급
- [x] `POST /api/auth/logout` — 리프레시 토큰 무효화
- [x] JWT 인증 필터 (Authorization 헤더 검증, SecurityContext 등록, access/refresh 타입 구분)
- [x] 예외 처리: `InvalidGoogleTokenException`, `InvalidRefreshTokenException`, `UnauthorizedException`
- [x] `GET /api/users/me`, `PATCH /api/users/me` — 닉네임 조회/설정(로그인 직후 필수 온보딩 + 마이페이지 수정 공용), 중복 허용
- [x] 로컬 MySQL 대상 부트업 테스트 완료

## Phase 3 — 관광지 데이터 적재 + 조회 ([api.md](./api.md) [관광지] 섹션)

- [ ] TourAPI 연동 배치/스크립트: 부산 지역 관광지 수집 → `TouristSpot` upsert (`tour_api_content_id` 기준)
- [ ] Naver Geocoding 연동: TourAPI 주소 → 좌표 보정 스켈레톤
- [ ] `category` enum 확정 후 분류 로직 (수동 매핑 또는 TourAPI `contentTypeId` 매핑 테이블)
- [ ] `GET /api/spots` — bounding box + category + mode 필터, 목록 조회
- [ ] `GET /api/spots/{spotId}` — 상세 조회
- [ ] 예외 처리: `InvalidBoundingBoxException`, `SpotNotFoundException`

## Phase 4 — QuietIndex 연동

- [ ] AI가 배치로 써주는 `quiet_index` 테이블 스키마 확정 (AI팀과 테이블/컬럼 형식 맞추기 — 같은 DB 공유인지, AI가 API로 백엔드에 밀어주는지 확인)
- [ ] `TouristSpot.current_quiet_score` / `quiet_score_updated_at` 캐시 갱신 로직 (신규 `quiet_index` insert 시 트리거 or 별도 배치)
- [ ] `/api/spots`, `/api/spots/{id}` 응답에 quietScore 필드 반영 확인

## Phase 5 — 대체지 추천 ([api.md](./api.md) [대체지 추천] 섹션)

- [ ] (블로커 해소 후) 배치 저장이면: AI가 쓴 `spot_alternative` 읽는 조회 로직만 구현
- [ ] (블로커 해소 후) 실시간 호출이면: AI 서버 API 클라이언트 구현, 타임아웃/장애 처리
- [ ] `GET /api/spots/{spotId}/alternatives` 구현
- [ ] 예외 처리: `SpotNotFoundException`, 대체지 없음 케이스 처리 방식 확정 후 반영

## Phase 6 — 방문 플로우 ([api.md](./api.md) [방문] 섹션)

- [ ] `POST /api/visits/start` — 방문 세션 생성, 중복 방문 체크(`AlreadyOngoingVisitException`)
- [ ] `PATCH /api/visits/{visitId}/complete` — 반경/체류시간 조건 검증 로직
  - [ ] 목적지 반경(예: 100m) 계산 (Haversine 또는 Geocoding API 활용)
  - [ ] 체류시간(예: 10분 이상) 검증
- [ ] 예외 처리: `VisitNotFoundException`, `InvalidVisitStateException`, `VisitConditionNotMetException`

---

## 우선순위 제안

1. Phase 1 (스캐폴딩) — 다른 모든 작업의 전제
2. Phase 2 (인증) + Phase 3 (관광지 조회) — AI 의존 없음, 병행 가능
3. 블로커 해소 (AI 협의) — Phase 4, 5 착수 전 필수
4. Phase 4 (QuietIndex 연동)
5. Phase 5 (대체지)
6. Phase 6 (방문 플로우) — 다른 Phase와 독립적이라 언제든 병행 가능
