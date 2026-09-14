# coltrip API 명세서

- Base URL: `/api` (예: `/api/spots`)
- 인증 정책은 현재 SecurityConfig와 JwtAuthenticationFilter 기준이다.
  - `POST /api/auth/google`: 사용자 JWT 불필요. 본문의 Google idToken은 필수.
  - `POST /api/auth/refresh`: 액세스 토큰 불필요. **Authorization: Bearer {refreshToken}은 필수**.
  - **모든 GET /api/spots/**: 비로그인 허용. 목록·상세·리뷰·관측/예측 타임라인·날짜별 추천·대체지 조회 포함.
  - 공개 GET에서 유효한 액세스 토큰은 개인화에 사용한다. 토큰 없음/만료/잘못된 토큰은 현재 필터에서 익명으로 처리하며, isLiked가 있는 응답은 false가 된다.
  - `/api/internal/**`: 사용자 JWT 대신 서버 간 `X-Internal-Api-Key` 검증. AI 호출용 `X-API-Key`와 구분한다.
  - Swagger UI 및 `/v3/api-docs/**`는 인증 없이 접근 가능하다.
  - 나머지는 액세스 토큰 인증 필수(방문, 내 정보, 좋아요·리뷰 쓰기 등).
- 문서화한 업무/입력 오류 응답은 `{"code": "...", "message": "..."}` 형식

**공통 에러 (모든 엔드포인트)**

| code | HTTP | 발생 조건 |
|---|---|---|
| `UnauthorizedException` | 401 | 토큰 없음/만료/위조 |
| `MissingParameterException` | 400 | 필수 쿼리 파라미터 누락 |
| `InvalidParameterException` | 400 | 쿼리 파라미터 타입/enum 값 오류 (예: `category=NOTEXIST`) |
| `InvalidRequestBodyException` | 400 | 요청 바디를 해석할 수 없음 (JSON 문법 오류, 바디 내 enum 값 오타 등) |
| `ValidationException` | 400 | 요청 바디 검증 실패 (예: 빈 닉네임, 필수 필드 누락) |
- 마지막 갱신: 2026-09-13 (현재 브랜치의 컨트롤러·DTO·보안 설정 기준)
- 스키마 참고: [schema.md](./schema.md)
- 관광지 기본정보·감성모드 적재: [spot-import-api-spec.md](./spot-import-api-spec.md) (`POST /api/internal/spots`, AI 전송 계약 협의 필요)

---

## [인증]

### 구글 로그인
```
POST /api/auth/google
```
프론트(Flutter)에서 구글 SDK로 받은 idToken과 진입 의도(intent)를 백엔드로 전달 → 서버가 구글에 검증 후 자체 JWT 발급. 로그인과 회원가입 플로우는 `intent`로 분리한다.

**Request**
```json
{
  "idToken": "string",
  "intent": "LOGIN"
}
```
`intent`: `LOGIN` 또는 `SIGNUP`.
- `LOGIN`: 이미 가입된 사용자만 로그인 처리
- `SIGNUP`: 가입되지 않은 사용자만 신규 생성 후 로그인 처리

**Response `200`** — `JwtTokenResponse`
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "isNewUser": true,
  "user": {
    "id": 1,
    "email": "user@gmail.com",
    "nickname": null,
    "visitCount": 0,
    "likeCount": 0,
    "currentVisitId": null
  }
}
```
최초 가입 시 `nickname`은 `null`. 구글 프로필 이름을 자동으로 채우지 않음 — 로그인 직후 닉네임 설정은 필수이므로, 프론트는 `nickname == null`이면 닉네임 설정 화면으로 이동시켜야 함 (`isNewUser` 여부와 무관하게 `nickname`이 없으면 항상 이동).

**Exception**
- `InvalidGoogleTokenException` (401) — idToken 검증 실패
- `UserNotRegisteredException` (404) — `intent=LOGIN`인데 가입된 사용자가 없음
- `AlreadyRegisteredUserException` (409) — `intent=SIGNUP`인데 이미 가입된 사용자임
- 동일 Google 계정의 동시 가입도 중복 저장 요청은 409로 반환한다. 성공한 가입의 토큰은 변경하지 않으며, 충돌 응답에는 토큰을 포함하지 않는다. 회원 생성과 토큰 저장은 함께 커밋/롤백한다. 관련 없는 DB 오류는 가입 중복으로 숨기지 않는다. 상세: `docs/signup-concurrency.md`.
- `InvalidRequestBodyException` (400) — `intent` enum 값 오류 등 요청 바디 해석 실패
- `ValidationException` (400) — `idToken` 누락/빈 값, `intent` 누락

---

### 토큰 재발급
```
POST /api/auth/refresh
```
**Request** — Header `Authorization: Bearer {refreshToken}`

헤더 누락·빈 값·Bearer 토큰 누락·잘못된 형식·유효하지 않은 토큰은 모두 `401`과 `{"code":"InvalidRefreshTokenException","message":"..."}` 형식으로 반환한다. 헤더는 API 계약상 필수이며, 서버 바인딩만 선택적으로 받아 기존 인증 검증으로 처리한다(#87).

**Response `200`** — `JwtTokenResponse` (accessToken, refreshToken 재발급)

리프레시 토큰은 원자적으로 교체한다. 동일 토큰의 동시 재발급은 하나만 성공하며 이전 토큰 재사용은 401이다. 신규 JWT에는 고유 `jti`가 포함된다. 로그아웃과의 처리 순서 및 기존 토큰 호환성은 [토큰 회전 정책](./refresh-token-rotation.md)을 참고한다.

**Exception**: `InvalidRefreshTokenException` (401) — 만료/위조/DB에 저장된 값과 불일치

---

### 로그아웃
```
POST /api/auth/logout
```
**Request** — Header `Authorization: Bearer {accessToken}`. 서버는 User의 `refresh_token`을 NULL 처리.

**Response `200`**: `"로그아웃 성공"`

**Exception**: `UnauthorizedException` (401)

---

## [사용자]

### 내 정보 조회
```
GET /api/users/me
```
**Request** — Header `Authorization: Bearer {accessToken}`

**Response `200`** — `UserResponse`
```json
{
  "id": 1,
  "email": "user@gmail.com",
  "nickname": "string",
  "visitCount": 3,
  "likeCount": 7,
  "currentVisitId": 10
}
```
- `visitCount`: **방문을 완료(`COMPLETED`)한 횟수.** 시작만 하고 완료하지 않은 방문은 제외
- `likeCount`: 좋아요한 장소 수
- `currentVisitId`: 현재 진행 중(`STARTED`)인 방문 ID. 없으면 `null` — `GET /api/visits/current`와 같은 기준

이 세 필드는 `UserResponse`를 쓰는 모든 응답(구글 로그인, 토큰 재발급, 내 정보 조회, 닉네임 수정)에 동일하게 포함된다.

**Exception**: `UnauthorizedException` (401)

---

### 닉네임 설정/수정
```
PATCH /api/users/me
```
로그인 직후 필수 설정 화면과, 이후 마이페이지에서의 수정 화면에서 공용으로 사용. 닉네임 중복 제약 없음(중복 허용).

**Request**
```json
{
  "nickname": "string"
}
```
`nickname`: 1~20자, 공백 불가

**Response `200`** — `UserResponse` (변경된 정보 반환)

**Exception**: `UnauthorizedException` (401)

---

### 대체 장소 알림 설정 조회
```
GET /api/users/me/notification-settings
```
고요지수 하락 시 대체 장소를 제안받을지 여부. 기본값 `true`(2026-09 확정, 사용자 단위 저장). 방문 중 제안 check/select에서 이 값을 확인한다. 공개 대체지 GET 수동 조회는 이 설정으로 차단하지 않는다. 설정 저장만으로 백그라운드 푸시가 동작하는 것은 아님 — 필요 시 기기 토큰 등록은 별도로 정의.

**Response `200`** — `NotificationSettingsResponse`
```json
{
  "alternativeNotificationEnabled": true
}
```

**Exception**: `UnauthorizedException` (401)

---

### 대체 장소 알림 설정 변경
```
PATCH /api/users/me/notification-settings
```
**Request**
```json
{
  "alternativeNotificationEnabled": false
}
```

**Response `200`** — `NotificationSettingsResponse` (변경된 정보 반환)

**Exception**: `UnauthorizedException` (401), `ValidationException` (400) — 필드 누락

---

### 회원 탈퇴
```
DELETE /api/users/me
```
**하드 삭제**(2026-08-19 확정) — User row와 연관된 Visit 이력을 모두 함께 삭제. 되돌릴 수 없음. iOS 앱스토어 심사 가이드라인(5.1.1(v)) 대응 목적으로 로그아웃과 함께 필수 제공.

**Request** — Header `Authorization: Bearer {accessToken}`

**Response `200`**: `"회원 탈퇴 완료"`

**Exception**: `UnauthorizedException` (401)

---

## [관광지]

감성 라벨: COZY(아늑), NATURAL(자연), URBAN(도시), VINTAGE(빈티지), EXOTIC(이국), VIBRANT(활기), SENSORY(감각), TRANQUIL(고요).
필드명 mode/modes는 유지한다. 구 라벨은 허용하지 않으며 [전환 절차](./emotion-label-migration.md)를 참고한다.

### 날짜/시간대별 추천 및 예측 타임라인

날짜별 추천, 별도 예측 타임라인, 예측 배치 수신 계약은
[예측 추천 API 명세](./forecast-api-spec.md)를 참고한다.
기존 지도·상세의 현재 점수와 최근 24시간 관측 이력은 유지한다.
예측 데이터가 없으면 현재 점수로 대체하지 않는다.
예측 수신 계약은 신규 제안이며 AI의 실제 전송 연결은 아직 필요하다.

> 관광지 **조회(GET)** 는 로그인 없이 호출 가능(2026-08-31 확정). 지도 둘러보기까지 로그인 벽을 세우면 이탈이 크고, 관광지 정보 자체는 공개 데이터이기 때문. 좋아요·리뷰 작성 등 쓰기 작업은 인증 필요.

### 목록 조회 (지도 히트맵 / 목록용)
```
GET /api/spots?swLat={}&swLng={}&neLat={}&neLng={}&category={}&mode={}
```
지도 화면에서 현재 보이는 영역(bounding box) 안의 장소를 감성모드·장소유형 필터와 함께 조회. `quietScore`가 응답에 포함되므로 별도 "고요지수 조회 API"는 없음.

> **초기 추천 검색 반경 15km**(2026-08-18 확정) — API 파라미터가 아니라 **프론트가 지도 초기 진입 시 bounding box를 설정하는 가이드값**. 사용자 현재 위치 기준 대략 15km 반경이 보이는 정도로 초기 줌/영역을 잡을 것.

**Request (query params)**

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `swLat`, `swLng` | Y | 지도 영역 남서쪽 좌표 |
| `neLat`, `neLng` | Y | 지도 영역 북동쪽 좌표 |
| `category` | N | 장소유형 enum (예: `CAFE`). 미지정 시 전체 |
| `mode` | N | 감성모드 enum (예: `NATURAL`). 미지정 시 전체 |

`mode`는 **필터 조건일 뿐**이며, 응답의 `modes` 필드에는 해당 장소가 가진 **모든 감성모드**가 담긴다. (예시: `mode=EXOTIC`으로 조회한 장소가 `["EXOTIC","SENSORY"]`를 갖고 있으면 둘 다 응답한다)

**Response `200`** — `SpotListResponse`
```json
{
  "spots": [
    {
      "id": 1,
      "name": "string",
      "address": "부산광역시 중구 ...",
      "category": "CAFE",
      "modes": ["TRANQUIL", "SENSORY"],
      "imageUrl": "https://...",
      "latitude": 35.15,
      "longitude": 129.06,
      "quietScore": 82,
      "quietLevel": "QUIET",
      "quietScoreUpdatedAt": "2026-08-17T09:00:00",
      "isLiked": false
    }
  ]
}
```
`quietLevel`은 `quietScore`에서 백엔드가 파생 계산하는 값(0~40 `CROWDED`, 41~70 `NORMAL`, 71~100 `QUIET`). 별도 저장값 아님, `quietScore`가 없으면(NULL) `quietLevel`도 `null`.
`isLiked`는 요청에 유효한 토큰이 있을 때만 본인의 좋아요 여부를 반영하고, 비로그인 요청은 항상 `false`. `GET /api/users/me/likes` 응답은 정의상 전부 `true`.

**Exception**: `InvalidBoundingBoxException` (400) — 좌표 범위 값 오류

---

### 상세 조회
```
GET /api/spots/{spotId}
```
**Response `200`** — `SpotDetailResponse`
```json
{
  "id": 1,
  "name": "string",
  "address": "string",
  "category": "CAFE",
  "modes": ["TRANQUIL"],
  "description": "string",
  "imageUrl": "string",
  "recommendReason": "조용한 골목 안쪽에 위치해 방문객이 적고, 사유하기 좋은 공간입니다",
  "latitude": 35.15,
  "longitude": 129.06,
  "quietScore": 82,
  "quietLevel": "QUIET",
  "quietScoreUpdatedAt": "2026-08-17T09:00:00",
  "visitRadiusMeters": 100,
  "isLiked": false
}
```

**Exception**: `SpotNotFoundException` (404)

---

### 고요지수 24시간 타임라인 조회
```
GET /api/spots/{spotId}/quiet-index/timeline
```
장소 상세 화면의 시간대별 그래프용. **최근 24시간 관측 이력**을 1시간 슬롯으로 묶어 반환한다 — 미래 예측값이 아니다. 별도 예측 조회 코드는 구현되어 있으며 [예측 명세](./forecast-api-spec.md)의 AI 전송 계약은 협의가 필요하다.

슬롯 하나에 관측값이 여러 건이면 가장 최신 값을 대표값으로 쓰고, 관측값이 없는 슬롯은 `quietScore`/`observedAt`이 `null`이다(0점이나 현재값으로 임의 대체하지 않음). 항상 24개 슬롯을 오래된 순 → 최신 순으로 반환하며, 슬롯 시각은 서버 로컬 시간(Asia/Seoul 가정) 기준 정시로 절삭된다.

**Response `200`** — `QuietIndexTimelineResponse`
```json
{
  "timeline": [
    {
      "slotStartAt": "2026-09-09T20:00:00",
      "quietScore": null,
      "observedAt": null
    },
    {
      "slotStartAt": "2026-09-10T19:00:00",
      "quietScore": 70,
      "observedAt": "2026-09-10T19:52:24.873406"
    }
  ]
}
```

**Exception**: `SpotNotFoundException` (404)

---

## [대체지 추천]

### 대체 장소 조회
```text
GET /api/spots/{spotId}/alternatives
```

비로그인 허용. 백엔드가 AI `POST /alternative`를 실시간 호출한다.
원래 목적지 좌표가 검색 기준점이며 사용자 현재 좌표 기준이 아니다.
한국 시간 hour/isWeekend, poiId 및 본인이 해당 장소를 STARTED 상태로 방문 중일 때 시작 점수를 전달한다(그 외 baselineQuietIndex=null).

AI 후보 거리 3km 이하와 백엔드 좌표로 재계산한 직선거리 3000m 이하를 모두 적용한다.
원래 장소, 미등록/매핑 불가 후보, 원래 목적지보다 고요지수가 높지 않은 후보를 제외한다.
중복 장소는 높은 score를 유지하고 score 내림차순으로 최대 3개 반환한다.
`spot_alternative` 테이블은 이 경로에서 읽거나 쓰지 않는다.

**Response 200** — `AlternativeListResponse`
```json
{
  "triggered": true,
  "targetQuietIndex": 32.5,
  "alternatives": [{
    "spot": {
      "id": 7,
      "name": "예시 장소",
      "address": "부산광역시 예시 주소",
      "category": "CAFE",
      "modes": ["TRANQUIL"],
      "imageUrl": null,
      "latitude": 35.16,
      "longitude": 129.05,
      "isLiked": false
    },
    "quietIndex": 91.2,
    "distanceKm": 1.44,
    "score": 0.87,
    "recommendReason": "더 한적한 장소입니다"
  }],
  "message": "주변의 더 한적한 대체 장소입니다."
}
```

- `score`: 0~1, 유사도에 이동 거리 감점을 적용한 **추천 점수**. `similarityScore` 필드는 제공하지 않는다.
- `targetQuietIndex`/`quietIndex`: AI의 0~100 소수점 점수. DB 현재 고요지수 캐시를 갱신하지 않는다.
- `distanceKm`: 백엔드 직선거리, 표시용 소수점 둘째 자리 반올림. 반경 판정은 반올림 전에 수행한다.
- 후보 장소 정보와 isLiked는 백엔드 DB 기준이다. 정상 응답은 Cache-Control: no-store.

**추천 없음도 200**이며 장애와 구분한다.
```json
{
  "triggered": true,
  "targetQuietIndex": 32.5,
  "alternatives": [],
  "message": "반경 3km 안에서 표시할 수 있는 등록된 대체 장소가 없습니다."
}
```

AI가 triggered=false이면 빈 배열과 "현재 장소는 대체지 제안 조건에 해당하지 않습니다."를 반환한다.
`triggered=true`라고 후보가 반드시 있는 것은 아니다.

| HTTP | code | 의미 |
|---|---|---|
| 404 | SpotNotFoundException | 원래 장소 없음 |
| 409 | AiPoiNotMapped | 원래 장소의 AI ID 매핑 불가 |
| 503 | AiNotConfigured | AI 주소/키 미설정 |
| 504 | AiTimeout | 연결/읽기 타임아웃 |
| 502 | AiConnectionFailed | AI 연결 실패 |
| 502 | AiRequestFailed | AI HTTP 오류 또는 응답 해석 실패 |
| 502 | AiInvalidResponse | 필수 필드/점수 범위/응답 일관성 오류 |

AI 장애를 빈 추천 목록으로 바꾸지 않는다. AI 타임아웃 기본값은 연결 3초, 읽기 30초다.
real 모드는 TourAPI contentId를 사용하고 SEED ID를 제외한다.
mock 모드는 코드에 정의된 SEED-008↔POI001, SEED-002↔POI004, SEED-001↔POI005, SEED-004↔POI008만 연결한다.
mock 역시 배포된 AI를 HTTP 호출하며 자체 가짜 응답을 생성하는 모드는 아니다.

---

## [방문]

### 방문 중 대체지 제안

`POST /api/visits/{visitId}/alternative-suggestion/check`, `/dismiss`, `/select`는 인증 필수다.
현재 40점 미만 또는 유효한 시작 점수 대비 15점 이상 하락을 check에서 평가한다.
방문 화면 진입/복귀 및 60초 간격 순차 호출을 전제로 하며 서버 자동 푸시는 아니다.
알림 허용, 관측값 신선도, 중복/만료 정책, 제안 상태 및 요청/응답은 [방문 중 제안 명세](./nudge-api-spec.md)를 따른다.
사용자 선택 시 select가 기존 방문 취소와 새 방문 시작을 원자적으로 수행한다.
방문 complete/cancel은 AI를 호출하지 않으므로 추천 장애가 방문 완료를 막지 않는다.


### 현재 방문 조회
```
GET /api/visits/current
```
로그인한 사용자의 진행 중(`STARTED`)인 방문을 조회한다. 앱 재실행 시 진행 중이던 방문 화면을 복원하는 용도. `GET /api/users/me` 등 `UserResponse`의 `currentVisitId`와 같은 기준(STARTED 여부)으로 판정되므로 같은 상태를 조회하면 일치한다. 두 HTTP 요청 사이에 시작/완료/취소/전환이 발생하면 결과가 달라질 수 있다.

**Response `200`** — `CurrentVisitResponse`. 진행 중인 방문이 없으면 `visit: null`.
```json
{
  "visit": {
    "visitId": 10,
    "spotId": 1,
    "spotName": "string",
    "spotAddress": "string",
    "category": "CAFE",
    "imageUrl": "string",
    "latitude": 35.15,
    "longitude": 129.06,
    "status": "STARTED",
    "startedAt": "2026-08-17T10:00:00",
    "startQuietScore": 40,
    "currentQuietScore": 32,
    "currentQuietLevel": "CROWDED",
    "visitRadiusMeters": 100
  }
}
```
방문이 없으면 정확히 `{"visit": null}`을 반환하며 404가 아니다. 완료/취소/타인 방문은 제외한다.
`startQuietScore`는 시작 시점 스냅샷, `currentQuietScore`와 `currentQuietLevel`은 DB의 현재 저장값이며 미수신이면 null이다.
`imageUrl`도 null일 수 있다. `visitRadiusMeters`는 완료 판정 반경(m)이며 체류시간 필드는 없다.
현재 값이 실시간 관측임을 보장하지 않는다. 제안 판정은 프론트의 단순 점수 비교가 아니라 아래 방문 중 제안 API를 사용한다.

**Exception**: `UnauthorizedException` (401)

---

### 방문 시작
```
POST /api/visits/start
```
"조용한 여행 시작하기" 버튼 → 위치 추적 세션 시작.

**Request**
```json
{
  "spotId": 1,
  "startLatitude": 35.15,
  "startLongitude": 129.06
}
```

**Response `200`** — `VisitStartResponse`
```json
{
  "visitId": 10,
  "status": "STARTED",
  "startedAt": "2026-08-17T10:00:00"
}
```

**Exception**: `SpotNotFoundException` (404), `AlreadyOngoingVisitException` (409) — 이미 진행 중인 방문이 있는 경우

---

### 방문 완료 처리
```
PATCH /api/visits/{visitId}/complete
```
목적지 반경 진입 시 프론트가 호출. 체류시간 조건은 없다(2026-09 제거 — 위변조 여지가 있고 시연 시 대기가 길어 반경 진입만으로 판정하도록 팀 확정). 반경은 카테고리별로 다름 — 점형 장소(카페/도서관/미술관/서점/사찰) **100m**, 면적형 장소(공원/해변/골목) **250m** (`Category.getVisitRadiusMeters()`, 잠정값·실측 검증 필요 — 장소 상세/현재 방문 조회 응답의 `visitRadiusMeters`로도 안내됨).

**Request**
```json
{
  "arrivedLatitude": 35.1502,
  "arrivedLongitude": 129.0601
}
```

**Response `200`** — `VisitCompleteResponse`
```json
{
  "visitId": 10,
  "status": "COMPLETED",
  "completedAt": "2026-08-17T10:15:00"
}
```

**Exception**: `VisitNotFoundException` (404), `InvalidVisitStateException` (409) — 이미 완료/취소된 방문, `VisitConditionNotMetException` (400) — 반경 조건 미충족

---

### 방문 취소
```
PATCH /api/visits/{visitId}/cancel
```
진행 중인 방문을 취소한다. 대체지 선택 등 목적지를 바꿀 때도 재사용 — 취소 후 바로 다른 장소로 방문을 다시 시작할 수 있다. 취소된 방문은 현재 방문 조회/`currentVisitId`에서 제외되고, `visitCount`(완료 횟수)에도 포함되지 않으며 리뷰 작성 대상도 아니다.

**Response `200`** — `VisitCancelResponse`
```json
{
  "visitId": 10,
  "status": "CANCELED"
}
```

**Exception**: `VisitNotFoundException` (404) — 존재하지 않거나 타인의 방문, `InvalidVisitStateException` (409) — 이미 완료/취소된 방문(반복 취소 포함)

---

### 방문 완료 이력 조회
```
GET /api/visits/history
```
본인이 완료(`COMPLETED`)한 방문을 완료순(최신 먼저)으로 조회. 마이페이지 "다녀온 곳" 목록용. `STARTED`/`CANCELED` 방문은 제외되며, 같은 장소를 여러 번 방문했다면 방문 건별로 각각 표시된다(장소 단위로 묶지 않음).

**Response `200`** — `VisitHistoryResponse`
```json
{
  "visits": [
    {
      "visitId": 10,
      "spotId": 1,
      "spotName": "string",
      "spotAddress": "string",
      "category": "CAFE",
      "imageUrl": "string",
      "startedAt": "2026-08-17T10:00:00",
      "completedAt": "2026-08-17T10:15:00",
      "reviewId": 4
    }
  ]
}
```
`reviewId`는 해당 방문에 작성된 리뷰의 id. 아직 리뷰를 작성하지 않았다면 `null` — 프론트는 이 값으로 "리뷰 쓰기"/"리뷰 보기" 버튼을 분기할 수 있다. 방문이 없으면 `visits: []`.

**Exception**: `UnauthorizedException` (401)

---

## [좋아요]

### 좋아요 등록
```
POST /api/spots/{spotId}/like
```
**멱등** — 이미 좋아요한 상태에서 다시 호출해도 200. 프론트에서 따닥 눌러도 에러가 안 남.

동시 등록도 사용자 행 잠금으로 직렬화해 모두 200을 반환하고 DB에는 한 건만 저장한다. 취소도 동일한 잠금을 사용한다. 리뷰 동시 작성은 한 건만 성공하고 나머지는 409를 반환한다. 검증 방법은 [동시 요청 처리](./like-review-concurrency.md)를 참고한다.

**Response `200`**
```json
{ "spotId": 1, "liked": true }
```

**Exception**: `UnauthorizedException` (401), `SpotNotFoundException` (404)

---

### 좋아요 취소
```
DELETE /api/spots/{spotId}/like
```
**멱등** — 좋아요하지 않은 상태에서 호출해도 200.

**Response `200`**
```json
{ "spotId": 1, "liked": false }
```

**Exception**: `UnauthorizedException` (401)

---

### 내가 좋아요한 장소 목록
```
GET /api/users/me/likes
```
**Response `200`** — `SpotListResponse` (`GET /api/spots`와 동일한 형태, 최근 좋아요순)

**Exception**: `UnauthorizedException` (401)

---

## [리뷰]

**별점(1~5) + 한줄평** 방식. (2026-09-01 변경: 기존 고요함 피드백 3단계에서 전환)

**작성 자격**: 해당 장소를 **방문 완료(`Visit.status == COMPLETED`)한 사용자만**, **방문 1건당 리뷰 1건**. 그래서 생성 엔드포인트가 `spots`가 아니라 `visits` 하위에 있다.

`rating`은 1~5 정수(필수), `content`는 선택이며 최대 300자.

### 리뷰 작성
```
POST /api/visits/{visitId}/review
```
**Request**
```json
{
  "rating": 5,
  "content": "평일 오후라 정말 조용했어요"
}
```

**Response `200`** — `ReviewResponse`
```json
{
  "id": 1,
  "spotId": 3,
  "userId": 4,
  "nickname": "테스터",
  "rating": 5,
  "content": "평일 오후라 정말 조용했어요",
  "createdAt": "2026-08-31T16:36:29",
  "updatedAt": "2026-08-31T16:36:29"
}
```

**Exception**
- `UnauthorizedException` (401)
- `VisitNotFoundException` (404) — 없는 방문이거나 **본인 방문이 아닌 경우**(존재 여부를 노출하지 않기 위해 403이 아닌 404로 통일)
- `ReviewNotAllowedException` (409) — 방문 미완료, 또는 해당 방문에 이미 리뷰를 작성함

---

### 장소별 리뷰 목록
```
GET /api/spots/{spotId}/reviews
```
로그인 없이 조회 가능. 최신순.

**Response `200`**
```json
{ "reviews": [ /* ReviewResponse 배열 */ ] }
```

---

### 리뷰 수정
```
PATCH /api/reviews/{reviewId}
```
본인이 작성한 리뷰만 수정 가능. **부분 수정이 아니라 매번 `rating`·`content`를 전체 재지정**한다(닉네임 수정 API와 동일한 정책) — `rating`은 필수(1~5), `content`를 생략하거나 명시적으로 `null`을 보내면 한줄평이 삭제된다. `createdAt`은 유지되고 `updatedAt`만 갱신된다.

**Request**
```json
{
  "rating": 4,
  "content": "다시 가보니 살짝 붐볐어요"
}
```

**Response `200`** — `ReviewResponse` (수정된 정보 반환)

**Exception**: `UnauthorizedException` (401), `ReviewNotFoundException` (404) — 없는 리뷰이거나 본인 리뷰가 아닌 경우(삭제 API와 동일하게 통일), `ValidationException` (400) — rating 범위/필수 위반, content 300자 초과

---

### 리뷰 삭제
```
DELETE /api/reviews/{reviewId}
```
본인이 작성한 리뷰만 삭제 가능.

**Response `200`**: `"리뷰 삭제 완료"`

**Exception**: `UnauthorizedException` (401), `ReviewNotFoundException` (404) — 없는 리뷰이거나 본인 리뷰가 아닌 경우

---

## [내부/AI 연동]

일반 사용자 인증(JWT)이 아니라 **AI 배치 서버 전용** 인증(`X-Internal-Api-Key` 헤더)을 사용. 이 값은 노션 API KEY 페이지에 별도로 관리, AI팀과 동일한 값 공유 필요.

### QuietIndex push (AI → 백엔드)
```
POST /api/internal/quiet-index
```
AI가 계산한 quietScore를 백엔드에 전달한다. 수신 코드는 구현되어 있지만 백엔드의 자동 1시간 스케줄러나 AI GET /quiet-index/map 수집기는 없다. 실제 공급 주기는 AI 운영 설정에서 확인해야 한다. `quiet_index` 이력 테이블에 저장 + `tourist_spot`의 캐시 컬럼(`current_quiet_score`, `quiet_score_updated_at`) 갱신.

**Request** — Header `X-Internal-Api-Key: {sharedSecret}`
```json
{
  "tourApiContentId": "126508",
  "quietScore": 82,
  "calculatedAt": "2026-08-18T15:00:00",
  "rawMetrics": null
}
```
`tourApiContentId`로 스팟을 식별(내부 `spotId` 아님 — AI는 TourAPI 원본 ID 기준으로 관리). `rawMetrics`는 선택, JSON 문자열. quietScore는 0~100 정수, calculatedAt은 필수 로컬 시각이며 공급자와 한국 시간 기준을 맞춘다.

정상 형식의 요청에서 `X-Internal-Api-Key` 누락·빈 값·불일치는 모두 `401`과 `{"code":"InvalidInternalApiKeyException","message":"..."}`을 반환한다. 서버 내부 키 설정이 비어 있어도 인증되지 않는다. 키를 자동 trim하거나 잘못된 값을 보정하지 않는다. 본문 파싱/필드 검증은 컨트롤러 호출 전 수행되므로 본문까지 잘못된 요청은 기존 400을 반환할 수 있다(#87).
동일 장소·calculatedAt 재전송은 이력을 정정한다. 과거 이력은 저장하되 현재 캐시를 과거 값으로 되돌리지 않는다.

`calculatedAt`은 서버 검증 시각(KST) 이하여야 한다. 허용 미래 오차는 0이며 초과하면 `400 InvalidObservationTimeException`으로 거부하고 이력/현재 점수를 변경하지 않는다. [시각 검증 및 기존 데이터 복구 절차](./observation-time-validation.md)를 참고한다.
응답 quietScore/quietLevel은 요청값이 아니라 갱신 후 유지된 최신 캐시 값이다.

**Response `200`**
```json
{
  "spotId": 1,
  "quietScore": 82,
  "quietLevel": "QUIET"
}
```

**Exception**: `InvalidInternalApiKeyException` (401), `SpotNotFoundException` (404) — 백엔드에 아직 없는 `tourApiContentId`인 경우

---

## 참고: API로 만들지 않기로 한 것

- **`GET /api/modes`** — 감성모드는 고정 enum이라 문서(`schema.md`)에 값만 정의, 프론트/백엔드/AI가 각자 상수로 관리
- **`GET /api/quiet-index`** — 별도 엔드포인트 없이 `/api/spots`, `/api/spots/{id}` 응답에 `quietScore` 필드로 포함

## 구현 상태 및 Swagger 대조

현재 상태는 [task.md](./task.md), 코드·Swagger 대조 근거는 [문서 검증 기록](./documentation-audit.md)을 참고한다.
기본정보 적재/예측 수신 코드 존재와 실제 AI 전송·운영 검증 완료는 구분한다.
