# 백엔드 구현 현황

기준: 2026-09-14, `origin/develop`의 `727bf28`을 반영한 문서 브랜치 소스 코드.
체크 완료는 **코드 구현 확인**을 의미하며 배포·운영 DB 적재·실제 AI/프론트 연동 검증 완료를 의미하지 않는다.
과거 단계 번호와 머지 대기 문구 대신 현재 상태를 관리한다.

## 구현 완료

### 프로젝트 및 인증
- [x] Spring Boot 4.1.0, Java 21, Gradle wrapper, MySQL/JPA 구성.
- [x] GitHub Actions backend CI: MySQL 8.4 서비스, Java 21, Gradle build(테스트 포함) 구성. 최근 실행 성공 여부는 CI에서 별도 확인.
- [x] Google idToken 검증, LOGIN/SIGNUP intent 분기, JWT 발급·재발급·로그아웃.
- [x] 내 정보 조회/닉네임 수정/회원 탈퇴, visitCount/likeCount/currentVisitId 공통 응답.
- [x] 사용자별 대체 장소 알림 허용 설정 저장·조회 및 제안 check/select 반영.
- [x] 공개 관광지 GET 정책과 인증 필요한 방문/쓰기 API 분리.
- [x] 리프레시 토큰 회전과 동시 재발급 직렬화(#83). [토큰 회전 명세](./refresh-token-rotation.md).
- [x] 회원정보 변경/탈퇴와 인증 토큰 변경의 사용자 잠금 통일(#97). [동시성 명세](./user-auth-concurrency.md).
- [x] 같은 Google 계정의 동시 가입 중복은 409, 회원/토큰 생성은 함께 롤백(#98). [가입 명세](./signup-concurrency.md).
- [x] 필수 인증 헤더 누락/빈 값의 401 처리(#87), 메서드/미디어 타입/없는 경로의 405/415/406/404 처리(#99). [공통 오류](./api.md).

### 관광지 적재·조회
- [x] 장소명 부분 일치 검색 GET /api/spots/search, 좌표 없는 페이지 조회, isLiked/누락 데이터 처리 및 통합 테스트. [검색 명세](./spot-search-api-spec.md).
- [ ] 검색창 프론트 연동 및 배포 DB의 검색 실행 계획/응답 시간 검증.
- [x] 지도 bounding box 및 category/mode 필터, 장소 상세, isLiked.
- [x] 부산 12곳 개발 시드 및 감성모드 SQL 파일. 실제 DB 적재 여부는 미확인.
- [x] POST /api/internal/spots: 내부 키 인증, tourApiContentId upsert, 감성모드 교체·중복 제거.
- [x] 기본정보 sourceUpdatedAt 순서 검증과 과거 스냅샷 무시, 기존 관광지 ID/연결 보존.
- [x] 기본정보 적재·재적재 테스트와 [수신 명세](./spot-import-api-spec.md).
- [ ] AI팀의 기본정보 전달 담당/방식 및 신규 sourceUpdatedAt 계약 확정, 실제 전송 검증.

### 현재 고요지수
- [x] POST /api/internal/quiet-index: X-Internal-Api-Key 인증, contentId로 기등록 장소 연결.
- [x] 동일 장소·계산 시각 이력 upsert, 최신 점수 캐시 갱신, 과거 값으로 캐시 덮어쓰기 방지.
- [x] 지도·상세 quietScore/quietLevel/quietScoreUpdatedAt 응답.
- [x] 최근 24시간 관측 타임라인. 누락 슬롯 null, 미래 예측과 분리.
- [x] AI GET /quiet-index/map 호출 클라이언트 및 설정 가능한 주기적 동기화(2026-09-13, PR #69/이슈 #63) — 매시 5분 스케줄러, 우리 DB에 있는 poiId만 반영, 소수점 점수는 반올림. 상세: [고요지수 지도 API 풀링 명세](./quiet-index-map-sync-spec.md)
- [x] 지도 GET의 is_weekend 쿼리 수정(#82), 응답 검증/중복 제외/건별 실패 격리(#85).
- [x] push calculatedAt의 미래 시각 거부(#86). [시각 검증](./observation-time-validation.md).
- [ ] 실제 운영 반영 후 매칭 건수·응답 시간 확인, AI 데이터 준비 시점과 호출 주기의 적절성 검증.
- [ ] 관측/계산 시각·출처 필드는 AI 응답에 없어 스케줄러 실행 시각으로 대체 중 — 계약 필요해지면 AI팀과 재확인.

### 대체지 및 방문 중 제안
- [x] GET /api/spots/{spotId}/alternatives → AI POST /alternative 실시간 호출.
- [x] 서버 주소/키 설정, 한국 시간 hour/isWeekend, real/mock ID 매핑.
- [x] 원래 목적지 기준 3km 제한, 자기 자신·중복·미등록 후보 제외, 더 고요한 후보 최대 3개.
- [x] score(0~1 추천 점수), 추천 이유, 장소 표시 정보, 빈 배열/message 반환.
- [x] 정상 추천 없음과 409/502/503/504 오류 구분. spot_alternative 미사용.
- [x] 방문 중 제안 check/dismiss/select, 40점 미만 또는 15점 이상 하락 조건.
- [x] null/오래된 점수, 알림 거부, 중복 제안/만료 처리 및 선택 시 방문 전환.
- [x] AI 호출과 방문 완료 분리, 관련 테스트. 상세는 [제안 명세](./nudge-api-spec.md).
- [ ] 실제 AI 데이터/인증/장소 매핑 및 프론트 폴링·선택 통합 검증.
- [ ] 백그라운드 푸시(#89): 전달 방식·기기 토큰·중복 정책 확정 및 구현. 현재 check API와 알림 허용 설정만으로 앱 종료 상태에 알림이 전송되지는 않음.

### 방문·좋아요·리뷰
- [x] 방문 시작/완료/취소, 사용자 행 잠금으로 상태 변경 직렬화.
- [x] GET /api/visits/current: 본인의 STARTED 방문, 없으면 visit=null.
- [x] GET /api/visits/history: 완료 이력과 reviewId.
- [x] 체류시간 조건 제거, 반경 진입으로 완료. 장소 상세/현재 방문 응답 visitRadiusMeters.
- [x] 점형 100m/면적형 250m 카테고리 반경 코드. 실제 적정성 검증은 별도.
- [x] 좋아요 등록/취소 및 내 좋아요 목록.
- [x] 완료 방문당 리뷰 1개, 별점 1~5, 작성/조회/수정/삭제, 본인 확인.
- [x] 동시 좋아요 등록/취소 직렬화, 동시 리뷰 작성 중복 409 처리(#84). [동시 요청 명세](./like-review-concurrency.md).
- [ ] 기존 DB의 review.quiet_feedback 잔존 여부 점검 및 승인된 스키마 정리.
- [ ] 방문 반경 실측 검증(#48).

### 예측 추천
- [x] quiet_forecast 저장 구조 및 내부 예측 배치 수신.
- [x] 날짜/시간 추천, 위치 기본값·반경·category/mode 필터, 정렬/개수 제한.
- [x] 24시간 예측 타임라인 및 관측 이력 분리. 없거나 만료된 예측은 현재값으로 대체하지 않음.
- [x] 예측 입력·필터·저장/조회 테스트 및 [예측 명세](./forecast-api-spec.md).
- [ ] AI 실제 날짜별 예측 가능 기간과 generatedAt/targetAt/validUntil/source/modelVersion 계약 확정.
- [ ] AI 전송 및 실제 MySQL/프론트 통합 검증.

## 남은 작업 순서
1. 기본정보 실제 데이터와 식별자/전송 계약 확정, 재적재 검증.
2. 지도 주기적 동기화 운영 반영 후 매칭률·응답 시간 확인.
3. 대체지·방문 제안의 실제 AI/프론트 통합 검증.
4. 날짜별 예측 계약 확정과 전송 연결.
5. 고정 도메인·HTTPS 배포 상태 확인 및 필요한 배포 작업.
6. 시드·레거시 컬럼 점검, 반경 실측 및 운영 검증.
7. 백그라운드 푸시(#89), 개인정보 보유기간·파기 정책 및 배치(#90) 별도 진행.

시드를 자동 삭제하지 않는다. visit/spot_like/review/quiet_index/quiet_forecast/spot_mode,
spot_alternative의 양쪽 FK와 방문 제안 JSON에 저장된 장소 ID까지 확인한 뒤 이전 정책을 정한다.
실제 DB 상태, 최근 CI 실행, 도메인·HTTPS 배포 상태는 이 문서 작업에서 확인하지 않았다.

