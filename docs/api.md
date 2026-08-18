# coltrip API 명세서

- Base URL: `/api` (예: `/api/spots`)
- 인증: `Authorization: Bearer {accessToken}` (로그인/토큰재발급 제외 전부 필요)
- 마지막 갱신: 2026-08-18
- 스키마 참고: [schema.md](./schema.md)

---

## [인증]

### 구글 로그인
```
POST /api/auth/google
```
프론트(Flutter)에서 구글 SDK로 받은 idToken을 백엔드로 전달 → 서버가 구글에 검증 후 자체 JWT 발급. 최초 로그인이면 User 자동 생성(회원가입 겸용).

**Request**
```json
{
  "idToken": "string"
}
```

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
    "profileImageUrl": "string"
  }
}
```
최초 가입 시 `nickname`은 `null`. 구글 프로필 이름을 자동으로 채우지 않음 — 로그인 직후 닉네임 설정은 필수이므로, 프론트는 `nickname == null`이면 닉네임 설정 화면으로 이동시켜야 함 (`isNewUser` 여부와 무관하게 `nickname`이 없으면 항상 이동).

**Exception**: `InvalidGoogleTokenException` (401) — idToken 검증 실패

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
  "profileImageUrl": "string"
}
```

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

## [관광지]

### 목록 조회 (지도 히트맵 / 목록용)
```
GET /api/spots?swLat={}&swLng={}&neLat={}&neLng={}&category={}&mode={}
```
지도 화면에서 현재 보이는 영역(bounding box) 안의 장소를 감성모드·장소유형 필터와 함께 조회. `quietScore`가 응답에 포함되므로 별도 "고요지수 조회 API"는 없음.

**Request (query params)**

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `swLat`, `swLng` | Y | 지도 영역 남서쪽 좌표 |
| `neLat`, `neLng` | Y | 지도 영역 북동쪽 좌표 |
| `category` | N | 장소유형 enum (예: `CAFE`). 미지정 시 전체 |
| `mode` | N | 감성모드 enum (예: `WALK`). 미지정 시 전체 |

**Response `200`** — `SpotListResponseDTO`
```json
{
  "spots": [
    {
      "id": 1,
      "name": "string",
      "category": "CAFE",
      "modes": ["CONTEMPLATION", "SCENERY"],
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

> ⚠️ [schema.md](./schema.md)에 적어둔 대로, 이 데이터가 배치 저장인지 실시간 AI 호출인지는 미확정. 아래는 배치 저장(스키마의 `spot_alternative` 테이블 조회) 기준으로 작성. 실시간으로 바뀌면 컨트롤러 내부 구현만 바뀌고 이 응답 스펙은 동일하게 유지 가능.

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
