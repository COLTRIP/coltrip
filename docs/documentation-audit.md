# 문서·Swagger 대조 기록

기준일: 2026-09-14. develop `727bf28`을 반영한 문서 브랜치 소스 기준이며 운영 배포 상태는 별도다.

## 확인 방법

컨트롤러의 매핑/Operation, 응답 record, 서비스 정책, 예외 처리 및 SecurityConfig를 대조했다.
이전 2026-09-13 작업에서는 별도 임시 프로젝트에 당시 Java 소스를 복사하고 테스트용 JWT와 H2(MySQL 모드)로 SpringBootTest/MockMvc를 실행해 GET /v3/api-docs 생성 및 필드 검증을 수행했다.
이번 2026-09-14 작업은 최신 DTO/컨트롤러의 Swagger 주석, 인증 설정, 예외 처리기 및 스케줄러와 문서의 정적 대조다. 최신 OpenAPI 생성 및 업무 회귀 테스트를 다시 실행한 결과로 해석하지 않는다.
실제 DB·비밀 설정·AI 서버를 사용하지 않았고 업무 로직/테스트 설정은 변경하지 않는다.

## 대조 결과

| 항목 | 확인 내용 |
|---|---|
| 현재 방문 | CurrentVisitResponse.visit, CurrentVisit.visitRadiusMeters 포함. STARTED만 조회, 없으면 visit=null |
| 내 정보 | UserResponse.currentVisitId 포함. 별도 HTTP 요청 사이 상태 변경은 가능 |
| 상세 | SpotDetailResponse.visitRadiusMeters/isLiked/quietScoreUpdatedAt 포함 |
| 완료 요청 | VisitCompleteRequest는 arrivedLatitude/arrivedLongitude만 받음. 체류시간 필드 없음 |
| 대체지 | AlternativeListResponse: triggered/targetQuietIndex/alternatives/message |
| 후보 | Alternative의 quietIndex/distanceKm/score/recommendReason과 spot. similarityScore 없음 |
| 인증 | 전체 GET /api/spots/** 공개. refresh는 사용자 accessToken 대신 refreshToken 헤더 필요 |
| 관측 수신 | 내부 키 인증, 동일 계산 시각 이력 정정, 과거 이력이 최신 캐시를 덮어쓰지 않음 |
| 관측 주기 | 수신 API와 공급 주기를 구분. 자동 1시간 갱신으로 오해되는 Swagger 설명 수정 |
| 범위 | 체류시간 제거/반경 필드는 이 브랜치에 이미 구현되어 있어 문서에 반영 |
| 지도 동기화 | 매시 5분 GET 수집 구현. is_weekend 쿼리, 건별 검증/실패 격리. 수집기가 없다는 api.md의 과거 설명 정정 |
| 관측 시각 | push의 미래 calculatedAt은 400. 지도 pull은 원천 시각 부재로 동기화 시작 시각을 저장하며 실제 관측 시각과 구분 |
| 인증 동시성 | 토큰 회전, 회원정보/토큰 변경 잠금 통일, 동시 가입 중복 409를 task.md에 반영 |
| 프로토콜 오류 | GlobalExceptionHandler의 404/405/406/415 및 헤더 보존 정책을 api.md와 대조 |

대체지 오류는 AiAlternativeClient/AlternativeDataReader/AlternativeController의 실제 처리 기준으로
404/409/502/503/504와 code를 api.md에 기록했다. 정상 빈 목록을 장애로 취급하지 않는다.
방문 중 제안은 NudgeController와 nudge-api-spec.md, 예측은 ForecastController와 forecast-api-spec.md를 연결했다.

## Swagger 표시의 한계

일부 컨트롤러에는 오류 응답 주석이 추가되어 있다. 예를 들어 AuthController.refresh와 InternalQuietIndexController.pushQuietIndex는 200/401을 명시한다. 다만 모든 경로의 실제 오류가 주석에 완전히 선언되어 있지는 않다.
설명의 409/502/503/504 등이 responses 항목에 개별 ApiResponse로 모두 선언된 상태는 아니다.
따라서 Swagger의 200 표시만으로 오류가 없다고 판단하거나, 이를 완전한 오류 응답 SDK 계약으로 간주하지 않는다.
오류는 api.md와 각 예외 처리기를 함께 확인한다. 전체 ApiResponse/nullable 정밀 주석 확장은 별도 정리 대상이다.

## 검증 범위 밖

- 실제 MySQL 데이터/마이그레이션/시드 참조, 최근 원격 CI 실행 결과.
- 실제 AI 인증·응답 및 운영 공급 주기, 날짜별 예측 생성·전송 계약.
- Docker Desktop 설치 상태, 실제 로컬 기동 및 원격 터널·고정 도메인·HTTPS 배포.
- 최신 OpenAPI JSON 생성 및 전체 업무 회귀 테스트 재실행. 이번 변경은 문서에 한정한다.

문서 체크리스트의 완료는 코드 구현을 의미하며 위 운영 항목의 완료를 뜻하지 않는다.
