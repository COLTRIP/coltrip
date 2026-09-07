# 방문 플로우 프론트 구조 (VisitingSpotPage)

"조용한 여행 시작하기" 이후 방문 중 화면의 상태 기반 UI 설계 메모.
관련 API: [api.md](./api.md)의 `[방문]` / `[대체지 추천]` 섹션, 판정 규칙은 [task.md](./task.md) 참고.

---

## 설계 방침

- **실시간 위치 추적 안 함.** 방문 중에는 목적지의 **고요지수를 주기적으로(약 5분) 재조회**해서
  방문 시작 시점보다 혼잡해졌으면 대체지 추천을 유도한다.
- **위치 권한은 "대체지 찾기"를 누르는 시점에만** 요청한다.
  - 방문 시작 시 현재 좌표 불필요 (네이버맵 딥링크가 현재 위치 기준으로 길안내).
  - 대체지 조회는 사용자 주변의 더 한적한 곳을 찾는 것이라 그때 위치가 필요.

---

## 화면 레이아웃

```
Scaffold (PopScope canPop:false — 방문 중 뒤로가기 차단)
└ body / SafeArea / Padding(24, 0, 24, 24)
  └ Column (spaceBetween)
    ├ Column (min)                  ← 상단 묶음
    │   ├ VisitingSpotCard(spot)    ← 스팟 정보 표시만, dumb
    │   ├ SizedBox(12)
    │   └ VisitingStatusBox(status) ← 상태별 안내 박스
    └ Column (min)                  ← 하단 액션 버튼 묶음
```

콘텐츠가 길어져 스크롤이 필요해지면 `spaceBetween` 방식은 깨짐 → 그때는 버튼을
`Scaffold.persistentFooterButtons`로 이동 (`bottomNavigationBar`는 향후 탭바 자리라 사용 금지).

---

## 진입: "방문 시작하기" (RecommendationDetailPage)

```dart
onPressed: () {
  // TODO: POST /api/visits/start
  Navigator.push(... VisitingSpotPage(spot: spot) ...);
}
```
권한 화면 안 거치고 바로 이동. (위치는 대체지 시점에만)

---

## 상태 정의 — `VisitStatus` (enum, `models/visit_status.dart`)

| 값 | 의미 | 진입 조건 |
|---|---|---|
| `visiting` | 방문 중 (평상시) | 기본값 |
| `crowdingDetected` | 고요지수 하락 감지 | 아래 트리거 |
| `completed` | 방문 완료 | 완료 액션 |

**트리거 (task.md 기준, 5분 폴링마다 평가)**
- 상대: `시작 시점 점수 - 현재 점수 >= 15`
- 절대: `현재 점수 < 40`
- 둘 중 하나라도 만족 → `crowdingDetected`, 상태 박스에서 "대체지 찾기" 유도

---

## `VisitingSpotViewModel` (ChangeNotifier, `view_models/`)

프로젝트 공통 패턴: 페이지(StatefulWidget)가 직접 생성 → `dispose()`에서 정리 →
`ListenableBuilder`로 구독. provider 패키지 미사용.

**보유 상태**
- `VisitStatus status`
- `startQuietScore` (생성 시 `spot.quietScore` 캡처), `currentQuietScore`
- `errorMessage`

**로직**
- 생성자에서 `Timer.periodic(5분)` 시작 → `_checkQuietScore()`
- `_checkQuietScore()`: `GET /api/spots/{id}`로 현재 고요지수 재조회 → 트리거 평가 → 상태 전환
- `findAlternatives()`: 위치 권한 확인/요청 후 `GET /api/spots/{id}/alternatives` → 결과 노출
- `completeVisit()`: 방문 완료 API 호출 + 타이머 정지 + `status = completed`
- `dispose()`: `_pollTimer?.cancel()`

---

## `VisitingStatusBox` (StatelessWidget, `views/visiting_spot/widgets/`)

- `status`만 받아 렌더. 로직 0.
- 바깥 `Container`(배경/패딩/모서리)는 고정, 내부 content만 `switch (status)`로 교체.
- 상태별 조각(`_VisitingContent` 등)도 전부 Stateless.
- (선택) `AnimatedSwitcher`로 전환 애니메이션 — content 위젯마다 `key` 부여.

```dart
child: switch (status) {
  VisitStatus.visiting         => const _VisitingContent(),
  VisitStatus.crowdingDetected => const _CrowdingContent(),  // "대체지 찾기" 버튼 포함
  VisitStatus.completed        => const _CompletedContent(),
},
```

---

## 위치 권한 (대체지 찾기 시점에만)

기존 `LocationPermissionPage` / `LocationPermissionViewModel` 재사용.
`findAlternatives()`에서 권한 없으면 안내 화면 push → 허용 후 대체지 조회.
권한/GPS API 상세는 [geolocator.md](./geolocator.md) 참고.

---

## TODO

- [ ] `VisitingSpotViewModel._checkQuietScore()` — 고요지수 재조회 API 연동
- [ ] `VisitingSpotPage` StatefulWidget 전환 + VM 바인딩
- [ ] `VisitingStatusBox` + 상태별 content 위젯
- [ ] `findAlternatives()` — 위치 권한 + `GET /api/spots/{id}/alternatives` + 결과 화면
- [ ] 방문 시작/완료 API 연동 (api.md `[방문]` 섹션 스펙 확정 후)
- [ ] 폴링 주기/트리거 수치 최종 확정 (지금 5분 / 15점 / 40점)
