# backend

coltrip 백엔드. Spring Boot(Gradle, Java 21) + MySQL.

## 실행 준비

1. 로컬 MySQL에 `coltrip` 데이터베이스 생성
2. `src/main/resources/application-secret.example.yml`을 같은 위치에 `application-secret.yml`로 복사 후 실제 값 채우기 (DB 계정, JWT secret, Google/Naver Client ID·Secret)
3. `./gradlew bootRun`

`application-secret.yml`은 `.gitignore`에 등록되어 있어 커밋되지 않습니다.

## 구조

```
domain/
  user/    User, Role
  spot/    TouristSpot, Category, Mode, QuietIndex, SpotMode, SpotAlternative
  visit/   Visit, VisitStatus
```

엔티티/필드 상세는 [../docs/schema.md](../docs/schema.md), API 스펙은 [../docs/api.md](../docs/api.md), 구현 진행 상황은 [../docs/task.md](../docs/task.md) 참고.
