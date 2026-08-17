# coltrip

AI 기반 실시간 혼잡도 분석으로 부산의 한적한 관광지를 추천하는 저밀도 정적 관광 플랫폼

## 구조

모노레포로 구성되어 있습니다.

```
.
├── backend/    # Spring Boot (Gradle, Java 21)
├── frontend/   # Flutter
└── docs/       # API 명세서, ERD/스키마, 백엔드 태스크
```

관련 문서: [docs/api.md](./docs/api.md) · [docs/schema.md](./docs/schema.md) · [docs/task.md](./docs/task.md)

## 브랜치 전략

- `main`: 배포용 브랜치
- `develop`: 통합 개발 브랜치
- `feature/*`: 기능 개발 브랜치 (`develop`에서 분기, `develop`으로 PR)

`main`, `develop` 모두 PR 리뷰 없이 직접 push할 수 없습니다.

## CI

- Backend: `backend/` 변경 시 Gradle 빌드
- Frontend: `frontend/` 변경 시 `flutter analyze` + 빌드
