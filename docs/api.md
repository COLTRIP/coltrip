# coltrip API 명세서

- Base URL: `/api` (예: `/api/spots`)
- 인증: 기본은 `Authorization: Bearer {accessToken}` (JWT). 아래 예외 3가지는 다른 방식이거나 인증이 없다.
  - 로그인(`POST /api/auth/google`), 토큰 재발급(`POST /api/auth/refresh`) — 인증 헤더 자체가 불필요(재발급은 액세스 토큰이 아니라 리프레시 토큰을 헤더에 넣음)
  - 관광지 **조회(GET)** — `GET /api/spots`, `GET /api/spots/{id}`, `GET /api/spots/{id}/quiet-index/timeline`, `GET /api/spots/{id}/reviews` — 비로그인 허용(2026-08-31 확정). 토큰이 있으면 `isLiked` 등 개인화 필드에 반영되지만 없어도 200 응답
  - 내부 연동(`/api/internal/**`) — JWT가 아니라 `X-Internal-Api-Key` 헤더(AI 배치 서버 전용, 프론트는 사용하지 않음)
  - 그 외 전부(쓰기 작업 포함) 인증 필요
- 모든 에러 응답은 `{"code": "...", "message": "..."}` 형식

**공통 에러 (모든 엔드포인트)**

| code | HTTP | 발생 조건 |
|---|---|---|
| `UnauthorizedException` | 401 | 토큰 없음/만료/위조 |
| `MissingParameterException` | 400 | 필수 쿼리 파라미터 누락 |
| `InvalidParameterException` | 400 | 쿼리 파라미터 타입/enum 값 오류 (예: `category=NOTEXIST`) |
| `InvalidRequestBodyException` | 400 | 요청 바디를 해석할 수 없음 (JSON 문법 오류, 바디 내 enum 값 오타 등) |
| `ValidationException` | 400 | 요청 바디 검증 실패 (예: 빈 닉네임, 필수 필드 누락) |
- 마지막 갱신: 2026-09 (백엔드 구현 현황 재정리)
- 스키마 참고: [schema.md](./schema.md)

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

**Response `200`** — `JwtTokenResponseDTO`
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
- `InvalidRequestBodyException` (400) — `intent` enum 값 오류 등 요청 바디 해석 실패
- `ValidationException` (400) — `idToken` 누락/빈 값, `intent` 누락

---

### 토큰 재발급
```
POST /api/auth/refresh
```
**Request** — Header `Authorization: Bearer {refreshToken}`

**Response `200`** — `JwtTokenResponseDTO` (accessToken, refreshToken 재발급)

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

**Response `200`** — `UserResponseDTO`
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

이 세 필드는 `UserResponseDTO`를 쓰는 모든 응답(구글 로그인, 토큰 재발급, 내 정보 조회, 닉네임 수정)에 동일하게 포함된다.

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

**Response `200`** — `UserResponseDTO` (변경된 정보 반환)

**Exception**: `UnauthorizedException` (401)

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
| `mode` | N | 감성모드 enum (예: `WALK`). 미지정 시 전체 |

`mode`는 **필터 조건일 뿐**이며, 응답의 `modes` 필드에는 해당 장소가 가진 **모든 감성모드**가 담긴다. (예: `mode=WATER_GAZING`으로 조회해도 흰여울문화마을은 `["SCENERY","WALK","WATER_GAZING"]` 전체가 응답됨)

**Response `200`** — `SpotListResponseDTO`
```json
{
  "spots": [
    {
      "id": 1,
      "name": "string",
      "address": "부산광역시 중구 ...",
      "category": "CAFE",
      "modes": ["CONTEMPLATION", "SCENERY"],
      "imageUrl": "https://...",
      "latitude": 35.15,
      "longitude": 129.06,
      "quietScore": 82,
      "quietLevel": "QUIET",
      "quietScoreUpdatedAt": "2026-08-17T09:00:00"
    }
  ]
}
```
`quietLevel`은 `quietScore`에서 백엔드가 파생 계산하는 값(0~40 `CROWDED`, 41~70 `NORMAL`, 71~100 `QUIET`). 별도 저장값 아님, `quietScore`가 없으면(NULL) `quietLevel`도 `null`.

**Exception**: `InvalidBoundingBoxException` (400) — 좌표 범위 값 오류

---

### 상세 조회
```
GET /api/spots/{spotId}
```
**Response `200`** — `SpotDetailResponseDTO`
```json
{
  "id": 1,
  "name": "string",
  "address": "string",
  "category": "CAFE",
  "modes": ["CONTEMPLATION"],
  "description": "string",
  "imageUrl": "string",
  "recommendReason": "조용한 골목 안쪽에 위치해 방문객이 적고, 사유하기 좋은 공간입니다",
  "latitude": 35.15,
  "longitude": 129.06,
  "quietScore": 82,
  "quietLevel": "QUIET",
  "quietScoreUpdatedAt": "2026-08-17T09:00:00"
}
```

**Exception**: `SpotNotFoundException` (404)

---

## [대체지 추천]

### 대체지 목록 조회
```
GET /api/spots/{spotId}/alternatives
```
`spotId`가 혼잡(quietScore 낮음)할 때, 유사한 분위기의 더 한적한 대체지를 조회. AI가 산출한 리스트+추천이유+유사도를 그대로 매핑.

> ⚠️ **아직 미구현** (`SpotAlternative` 엔티티만 존재, 컨트롤러/서비스 없음). 저장 방식(배치 vs 실시간 호출)은 **2026-08-18 실시간 AI 호출로 팀 확정됨** — `schema.md`의 배치 저장 가정(`spot_alternative` 테이블)은 재설계 대상. 아래 응답 스펙은 그대로 유효하나, 반경 캡(3km)·빈 배열일 때 안내 문구 등 세부 파라미터는 여전히 미정(`task.md` 참고).

**Response `200`** — `AlternativeSpotListResponseDTO`
```json
{
  "alternatives": [
    {
      "spot": {
        "id": 7,
        "name": "string",
        "category": "CAFE",
        "latitude": 35.16,
        "longitude": 129.05,
        "quietScore": 91
      },
      "similarityScore": 0.87,
      "recommendReason": "비슷한 사유 분위기를 가지면서 현재 더 한적한 공간입니다"
    }
  ]
}
```

**Exception**: `SpotNotFoundException` (404), `NoAlternativeFoundException` (200 + 빈 배열로 처리할지, 404로 할지 팀 확인 필요)

---

## [방문]

### 현재 방문 조회
```
GET /api/visits/current
```
로그인한 사용자의 진행 중(`STARTED`)인 방문을 조회한다. 앱 재실행 시 진행 중이던 방문 화면을 복원하는 용도. `GET /api/users/me` 등 `UserResponseDTO`의 `currentVisitId`와 같은 기준(STARTED 여부)으로 판정되므로 두 값은 항상 일치한다.

**Response `200`** — `CurrentVisitResponseDTO`. 진행 중인 방문이 없으면 `visit: null`.
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
    "currentQuietLevel": "CROWDED"
  }
}
```
`startQuietScore`는 방문 시작 시점의 스냅샷(고요지수 하락 트리거 비교 기준), `currentQuietScore`/`currentQuietLevel`은 현재 시점 값 — 두 값의 차이로 프론트가 "혼잡해졌어요" 같은 안내를 만들 수 있다.

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

**Response `200`** — `VisitStartResponseDTO`
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
목적지 반경 진입 + 체류시간 조건 충족 시 프론트가 호출. 체류시간은 카테고리 무관 **10분(600초)** 고정. 반경은 카테고리별로 다름 — 점형 장소(카페/도서관/미술관/서점/사찰) **100m**, 면적형 장소(공원/해변/골목) **250m** (`Category.getVisitRadiusMeters()`, 잠정값·팀 확정 필요).

**Request**
```json
{
  "arrivedLatitude": 35.1502,
  "arrivedLongitude": 129.0601,
  "stayDurationSeconds": 620
}
```

**Response `200`** — `VisitCompleteResponseDTO`
```json
{
  "visitId": 10,
  "status": "COMPLETED",
  "completedAt": "2026-08-17T10:15:00"
}
```

**Exception**: `VisitNotFoundException` (404), `InvalidVisitStateException` (409) — 이미 완료/취소된 방문, `VisitConditionNotMetException` (400) — 반경/체류시간 조건 미충족

---

## [좋아요]

### 좋아요 등록
```
POST /api/spots/{spotId}/like
```
**멱등** — 이미 좋아요한 상태에서 다시 호출해도 200. 프론트에서 따닥 눌러도 에러가 안 남.

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
**Response `200`** — `SpotListResponseDTO` (`GET /api/spots`와 동일한 형태, 최근 좋아요순)

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

**Response `200`** — `ReviewResponseDTO`
```json
{
  "id": 1,
  "spotId": 3,
  "userId": 4,
  "nickname": "테스터",
  "rating": 5,
  "content": "평일 오후라 정말 조용했어요",
  "createdAt": "2026-08-31T16:36:29"
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
{ "reviews": [ /* ReviewResponseDTO 배열 */ ] }
```

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
AI가 배치(1시간 주기)로 계산한 quietScore를 백엔드에 전달. `quiet_index` 이력 테이블에 저장 + `tourist_spot`의 캐시 컬럼(`current_quiet_score`, `quiet_score_updated_at`) 갱신.

**Request** — Header `X-Internal-Api-Key: {sharedSecret}`
```json
{
  "tourApiContentId": "126508",
  "quietScore": 82,
  "calculatedAt": "2026-08-18T15:00:00",
  "rawMetrics": null
}
```
`tourApiContentId`로 스팟을 식별(내부 `spotId` 아님 — AI는 TourAPI 원본 ID 기준으로 관리). `rawMetrics`는 선택, JSON 문자열.

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

## 확정됐지만 구현 반영 전 (2026-08-18 회의)

- [x] `category` enum 8종 확정 — 코드/문서 반영 완료
- [x] 대체지 추천: **실시간 AI 호출**로 확정 — 아래 [대체지 추천] 섹션 재설계 예정 (미착수)
- [x] 대체지가 하나도 없을 때: **빈 배열 + 안내 멘트** — 응답 스펙 반영 예정 (미착수)
- [x] `similarityScore`: **0~1** 스케일 확정 — 반영 예정 (미착수)
