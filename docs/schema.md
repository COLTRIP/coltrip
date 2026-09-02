# coltrip ERD / 스키마 설계

- DB: MySQL
- 마지막 갱신: 2026-08-18
- 이 문서는 [api.md](./api.md)의 Request/Response DTO와 1:1로 대응됩니다.

## 설계 전제 (팀 확정 사항)

| 항목 | 결정 | 비고 |
|---|---|---|
| 인증 | 구글 로그인 + JWT | 소셜 로그인 ID, 리프레시 토큰을 User에 보관 |
| QuietIndex 계산 | AI가 배치(**1시간 주기**)로 계산 → `POST /api/internal/quiet-index`로 push | 하락 트리거 무의미해지면 주기 재논의 (2026-08-18 확정) |
| QuietIndex 저장 구조 | **이력 저장** (`quiet_index` 별도 테이블) | 향후 시계열 예측(정적 골든타임 가이드) 대비 |
| 감성모드(Mode) | 고정 enum, 5종 | 기획서 "주요 추천 유형" 기준 |
| 장소유형(Category) | 커스텀 enum, **8종 확정** (2026-08-18) | "기타" 카테고리는 두지 않음. TourAPI 수집 중 특정 유형이 많이 확인되면 새 카테고리 추가로 확장 |
| 대체지(Alternative) | AI가 리스트+추천이유+유사도 포함해서 전달 | QuietIndex와 동일하게 배치 저장으로 가정 (아래 "확인 필요" 참고) |

---

## 1. User

구글 소셜 로그인 사용자 정보 + JWT 리프레시 토큰 관리.

**회원 탈퇴 = 하드 삭제** (2026-08-19 확정): `DELETE /api/users/me` 호출 시 User row와 연관 데이터를 모두 삭제. `review`가 `visit`을 참조하므로 **review → spot_like → visit → user 순서**로 지워야 FK 제약에 걸리지 않는다. `tourist_spot`, `quiet_index` 등 공용 데이터는 영향 없음.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT PK | |
| `google_sub` | VARCHAR(255) UNIQUE | 구글 idToken의 `sub` 클레임 (소셜 로그인 식별자) |
| `email` | VARCHAR(255) UNIQUE | |
| `nickname` | VARCHAR(50) NULL | 가입 시 비어있음(구글 프로필 이름 자동 채움 없음). 로그인 직후 필수 설정 화면에서 입력, 이후 마이페이지에서 수정 가능. 중복 허용 |
| `refresh_token` | VARCHAR(500) NULL | 최신 발급 리프레시 토큰 (재발급 시 갱신, 로그아웃 시 NULL) |
| `role` | ENUM('USER') | 확장 대비, 현재는 단일 값 |
| `created_at` | DATETIME | |
| `updated_at` | DATETIME | |

---

## 2. TouristSpot

한국관광공사 TourAPI 원본 데이터를 정제해서 저장하는 관광지 기본 정보.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT PK | |
| `tour_api_content_id` | VARCHAR(50) UNIQUE | TourAPI 원본 `contentId` (재수집 시 upsert 키) |
| `name` | VARCHAR(200) | |
| `address` | VARCHAR(300) | |
| `latitude` | DECIMAL(10,7) | Naver Geocoding으로 보정된 좌표 |
| `longitude` | DECIMAL(10,7) | |
| `category` | ENUM(아래 표 참고) | 장소 유형 (탭 필터 기준) |
| `description` | TEXT NULL | 소개글 |
| `image_url` | VARCHAR(500) NULL | |
| `recommend_reason` | VARCHAR(500) NULL | AI가 산출한 추천 이유 (감성 맥락 기반 추천 기능용) |
| `current_quiet_score` | INT NULL | 최신 고요지수 **캐시** (아래 `quiet_index` 이력 테이블의 최신값 비정규화 저장, 조회 성능용) |
| `quiet_score_updated_at` | DATETIME NULL | 위 캐시값의 계산 시각 |
| `created_at` | DATETIME | |
| `updated_at` | DATETIME | |

**`category` enum (8종 확정, 2026-08-18)** — "기타" 카테고리 없음. TourAPI 수집 시 이 8종에 안 맞는 스팟은 수집 대상에서 제외:

```
CAFE(카페), PARK(공원), LIBRARY(도서관), GALLERY(미술관/전시),
BOOKSTORE(서점), TEMPLE(사찰), BEACH(해변), ALLEY(골목/거리)
```

> 확장 가능: TourAPI 수집 중 특정 유형 데이터가 많이 확인되면 새 카테고리 추가로 늘릴 수 있음(고정 아님).

**`current_quiet_score` / `quiet_score_updated_at`를 왜 중복 저장하나:** `quiet_index` 이력 테이블만 있으면 지도 목록 조회할 때마다 스팟별로 "가장 최근 값" 서브쿼리를 돌려야 해서 느림. 배치 계산 시 이 두 컬럼도 같이 갱신해주는 방식(쓰기 시점에 비정규화)으로 조회 성능을 확보.

**`quietLevel`(혼잡/보통/고요)은 컬럼으로 저장하지 않음** — `current_quiet_score` 기준 0~40 `CROWDED`, 41~70 `NORMAL`, 71~100 `QUIET`로 API 응답 시점에 파생 계산(`TouristSpot.getQuietLevel()`). 구간값은 AI 실제 점수 분포 확인되면 재조정 가능.

---

## 3. quiet_index (QuietIndex 이력)

AI가 배치로 계산한 고요지수 원본 이력. TouristSpot의 캐시 컬럼은 이 테이블의 최신 행을 복사한 것.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT PK | |
| `spot_id` | BIGINT FK → tourist_spot.id | |
| `quiet_score` | INT | 0~100 (기획서: "이해하기 쉬운 점수 형태로 변환") |
| `raw_metrics` | JSON NULL | 계산에 쓰인 원본 지표 스냅샷(유동인구, 면적 대비 밀도 등) — 디버깅/모델 개선용, MVP에서는 생략 가능 |
| `calculated_at` | DATETIME | 배치 계산 시각 |

인덱스: `(spot_id, calculated_at DESC)` — 특정 장소의 최신/이력 조회용.

---

## 4. spot_mode (Spot ↔ 감성모드 매핑)

한 장소가 여러 감성모드에 동시에 해당할 수 있어 N:M 매핑 테이블로 설계 (예: 어떤 공원이 '산책'이면서 '풍경 감상'에도 해당).

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT PK | |
| `spot_id` | BIGINT FK → tourist_spot.id | |
| `mode` | ENUM (아래) | |

**`mode` enum (팀 확정: 5종, "주요 추천 유형" 기준)**

```
WALK(산책), CONTEMPLATION(사유·명상), SCENERY(풍경 감상),
WATER_GAZING(물멍), CULTURE(조용한 문화·전시)
```

유니크 제약: `(spot_id, mode)` 조합 unique.

---

## 5. spot_alternative (대체지 추천, 배치 저장)

AI가 계산한 "이 장소가 혼잡할 때 추천할 대체지" 목록. QuietIndex와 동일하게 배치 계산 후 저장하는 것으로 가정함.

> ⚠️ **확인 필요**: 지금까지 나온 결정은 "AI가 리스트+추천이유+유사도를 준다"는 응답 형태에 대한 것이지, 이걸 QuietIndex처럼 **배치로 미리 계산해서 저장**해두는 건지, 아니면 사용자가 조회하는 시점에 AI 서버를 **실시간 호출**해서 받아오는 건지는 명시적으로 정해진 바가 없음. 이 스키마는 배치 저장을 전제로 설계했음 — 만약 실시간 호출이 맞다면 이 테이블은 필요 없고 API 레이어에서 AI 서버 응답을 그대로 패스스루하면 됨. **다음 AI 협의에서 확정 필요.**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT PK | |
| `origin_spot_id` | BIGINT FK → tourist_spot.id | 혼잡한 원래 목적지 |
| `alternative_spot_id` | BIGINT FK → tourist_spot.id | 추천 대체지 |
| `similarity_score` | DECIMAL(5,2) | AI가 산출한 유사도 (0~100 또는 0~1, AI팀과 스케일 통일 필요) |
| `recommend_reason` | VARCHAR(500) | AI가 준 추천 이유 텍스트 |
| `calculated_at` | DATETIME | |

인덱스: `(origin_spot_id, similarity_score DESC)`.

---

## 6. visit (방문 시작/완료)

"방문 시작" 버튼 플로우 지원용.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT PK | |
| `user_id` | BIGINT FK → user.id | |
| `spot_id` | BIGINT FK → tourist_spot.id | |
| `status` | ENUM('STARTED','COMPLETED','CANCELED') | |
| `start_latitude` / `start_longitude` | DECIMAL(10,7) | 방문 시작 버튼 누른 시점 위치 |
| `start_quiet_score` | INT NULL | 방문 시작 시점 목적지의 quietScore 스냅샷. 고요지수 하락 트리거(상대 기준)의 비교 기준값 |
| `started_at` | DATETIME | |
| `arrived_at` | DATETIME NULL | 목적지 반경 진입 확인 시각 |
| `completed_at` | DATETIME NULL | 체류시간 조건 충족 후 완료 처리 시각 |

---

## 7. spot_like (좋아요)

"유저가 좋아요한 장소"는 User의 컬럼이 아니라 매핑 테이블로 관리한다(한 유저가 여러 장소를 누르므로).

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT PK | |
| `user_id` | BIGINT FK → user.id | |
| `spot_id` | BIGINT FK → tourist_spot.id | |
| `created_at` | DATETIME | 좋아요 목록 정렬 기준(최신순) |

유니크 제약: `(user_id, spot_id)` — 같은 장소를 중복으로 좋아요할 수 없음. API는 멱등하게 동작.

---

## 8. review (별점 리뷰)

별점(1~5)과 한줄평을 받는다. (2026-09-01 변경: 기존 고요함 피드백 3단계 방식에서 일반 별점 방식으로 전환)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT PK | |
| `visit_id` | BIGINT FK → visit.id, **UNIQUE** | 작성 자격의 근거. unique 제약으로 "방문 1건당 리뷰 1건"을 DB 레벨에서 보장 |
| `user_id` | BIGINT FK → user.id | visit에서 파생(조회 편의를 위한 비정규화) |
| `spot_id` | BIGINT FK → tourist_spot.id | visit에서 파생(장소별 리뷰 조회용) |
| `rating` | INT | 1~5 별점 |
| `content` | VARCHAR(300) NULL | 한줄평(선택) |
| `created_at` / `updated_at` | DATETIME | |

**작성 조건**: `visit.status == COMPLETED` 이고 `visit.user_id == 작성자`.

---

## ERD 관계 요약

```
User 1───N Visit N───1 TouristSpot
User 1───N spot_like N───1 TouristSpot
User 1───N review N───1 TouristSpot
Visit 1───1 review
TouristSpot 1───N quiet_index
TouristSpot 1───N spot_mode
TouristSpot 1───N spot_alternative (origin_spot_id)
TouristSpot 1───N spot_alternative (alternative_spot_id)
```

## 미확정 / 팀 확인 필요 목록 (2026-08-18 기준)

- [x] `category`(장소유형) enum 최종 값 — 8종 확정
- [x] `similarity_score` 스케일 — 0~1 확정
- [x] QuietIndex 배치 계산 주기 — 1시간 확정, `POST /api/internal/quiet-index`로 push 받는 구조 구현 완료
- [ ] `spot_alternative`를 배치 저장할지, 실시간 AI 호출로 할지 — **실시간 호출로 결정됨(회의 확정), 아직 이 문서/코드에 미반영** — 재설계 예정(9b)
- [ ] TourAPI 관광지 기본정보 자체를 AI가 어떤 방식으로 백엔드에 전달할지 (push API/직접 DB/파일) — `task.md` Phase 3 참고
