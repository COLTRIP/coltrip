# 개발 환경 가이드 (백엔드)

로컬에서 서버를 띄우고, 프론트가 API를 테스트할 수 있게 하는 방법.

## 0. 사전 준비

**Java 21**이 필요하다 (`build.gradle`의 toolchain 설정). 확인:
```bash
java -version
```
21이 아니면 [Temurin 21](https://adoptium.net/temurin/releases/?version=21)이나 SDKMAN(`sdk install java 21-tem`)으로 설치. `./gradlew`는 Gradle wrapper라 Gradle 자체를 따로 설치할 필요는 없다 — JDK 21만 있으면 된다.

**MySQL**은 둘 중 편한 쪽으로 준비한다.

- **A. 로컬 설치** — Mac은 `brew install mysql && brew services start mysql`, Windows는 [MySQL Installer](https://dev.mysql.com/downloads/installer/)로 설치 후 서비스 시작. 설치 후 root 비밀번호를 기억해둘 것(1번 단계에서 씀).
- **B. Docker** — 로컬에 MySQL을 깔고 싶지 않다면:
  ```bash
  docker run -d --name coltrip-mysql \
    -e MYSQL_ROOT_PASSWORD=root \
    -e MYSQL_DATABASE=coltrip \
    -p 3306:3306 \
    mysql:8.4
  ```
  이 방법은 `CREATE DATABASE` 단계(1번 참고)를 건너뛰어도 된다 — `MYSQL_DATABASE` 환경변수가 컨테이너 최초 기동 시 자동으로 만들어준다. `application-secret.yml`의 `password`는 `root`로 채우면 된다.

### Windows 참고사항

- `./gradlew`는 PowerShell·cmd 어디서든 그대로 동작한다(별도로 `gradlew.bat`을 쓸 필요 없음 — Gradle wrapper가 OS를 알아서 판별).
- 이 문서와 [api.md](./api.md)의 예제는 macOS/Linux 기준 `curl`이다. **PowerShell의 `curl`은 `Invoke-WebRequest`의 별칭이라 옵션 문법이 다르다** — 예제가 안 될 경우:
  - Git Bash(Windows용 Git 설치 시 기본 포함)에서 실행하면 예제 그대로 동작한다. 가장 간단한 방법.
  - 또는 PowerShell 문법으로 변환: `curl -X POST url -H "Authorization: Bearer X"` → `Invoke-RestMethod -Method Post -Uri url -Headers @{Authorization="Bearer X"}`
- MySQL CLI 경로가 PATH에 없으면 winget/설치 시 "Add to PATH" 옵션을 켜거나, MySQL Workbench의 GUI로 1번 단계의 `CREATE DATABASE`를 실행해도 된다.

## 1. 최초 세팅

```bash
# 1) MySQL에 DB 생성
CREATE DATABASE coltrip CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2) 시크릿 설정 파일 준비
cp backend/src/main/resources/application-secret.example.yml \
   backend/src/main/resources/application-secret.yml
# → DB 계정, jwt.secret, oauth/naver 키, internal.api-key 채우기
# (이 파일은 .gitignore 대상이라 커밋되지 않음)
```

## 2. 서버 실행

```bash
cd backend
./gradlew bootRun
```

`Started BackendApplication` 로그가 뜨면 정상. 끄려면 `Ctrl + C`.

**팀 컨벤션은 8090이다** — `application-secret.example.yml`에 `server.port: 8090`이 이미 들어있어서, 1번 단계에서 그대로 복사했다면 별도 설정 없이 8090으로 뜬다. 이 문서의 모든 curl/Swagger 예제도 8090을 기준으로 쓰여 있다. (`application.yml`의 진짜 기본값은 8080이지만, `application-secret.yml` 쪽이 우선순위가 높아 최종적으로 8090이 적용된다 — `spring.config.import`로 가져온 설정이 이를 가져온 파일보다 우선순위가 높기 때문) 로그에 `Tomcat started on port ...`로 실제 포트가 찍히니 확인할 것.

### 다른 포트를 쓰고 싶다면

다른 프로젝트가 8090을 쓰고 있다면 `application-secret.yml`의 `server.port` 값을 원하는 포트로 바꾸면 된다. 이 파일은 `.gitignore` 대상이라 팀 설정에 영향을 주지 않는다. 단, 이 문서의 curl 예제들은 여전히 8090으로 적혀 있으니 본인 포트에 맞게 바꿔 읽을 것.

일회성으로만 바꾸려면 실행 인자를 써도 된다:
```bash
./gradlew bootRun --args='--server.port=8091'
```

## 3. 시드 데이터 적재

관광지 데이터를 AI 파이프라인으로 받기 전까지, 프론트 지도 화면 개발용 임시 데이터.

```bash
mysql -h 127.0.0.1 -u root -p coltrip < backend/seed/seed-spots.sql
```

부산 관광지 12곳 + 감성모드 매핑이 들어간다. `tour_api_content_id`가 `SEED-`로 시작하므로 나중에 한 번에 정리 가능:

```sql
DELETE FROM tourist_spot WHERE tour_api_content_id LIKE 'SEED-%';
```

## 4. 테스트 계정 / 토큰 발급

Google OAuth Android·iOS 클라이언트 등록(이슈 #9)이 끝나기 전에는 실제 구글 로그인을 할 수 없다. 그동안 인증이 필요한 API를 테스트하려면 아래 스크립트로 테스트 계정과 리프레시 토큰을 만든다.

```bash
./backend/seed/create-test-user.sh
```

출력된 리프레시 토큰으로 액세스 토큰을 받는다:

```bash
curl -X POST http://localhost:8090/api/auth/refresh \
  -H "Authorization: Bearer <리프레시 토큰>"
```

응답의 `accessToken`을 이후 요청 헤더에 사용:

```bash
curl http://localhost:8090/api/users/me \
  -H "Authorization: Bearer <accessToken>"
```

### ⚠️ 리프레시 토큰은 1회용이다

`/api/auth/refresh`를 호출하면 **새 리프레시 토큰이 함께 발급되고 이전 토큰은 즉시 무효화**된다(rotation). 응답의 `refreshToken`을 반드시 저장해서 다음 재발급에 써야 한다. 꼬이면 스크립트를 다시 실행하면 된다.

### ⚠️ 토큰은 커밋하지 않는다

이 레포는 public이다. 발급된 토큰은 자격 증명이므로 코드·문서·PR 어디에도 넣지 말 것.

## 5. Swagger UI (API 문서 · 테스트)

서버를 띄운 뒤 브라우저에서 접속:

```
http://localhost:8090/swagger-ui/index.html
```

터널로 공개했다면 `https://<터널주소>/swagger-ui/index.html`로도 접근 가능하다.

### 인증이 필요한 API 테스트하는 법

1. **인증 → 액세스 토큰 재발급** 을 펼친다.
2. 우측 상단 **Authorize** 버튼에 **리프레시 토큰**을 넣고 Authorize.
   (`Bearer ` 접두어 없이 토큰 값만 입력)
3. `POST /api/auth/refresh`를 **Try it out → Execute** 한다.
4. 응답의 `accessToken`을 복사한다.
5. 다시 **Authorize** 버튼을 눌러 이번엔 **accessToken**으로 교체한다.
6. 이제 자물쇠 표시가 있는 API를 호출할 수 있다.

> ⚠️ 3번에서 받은 응답의 `refreshToken`도 새 값으로 갱신되었으므로(rotation), 다음 재발급 때는 그 값을 써야 한다.

관광지 조회(GET)는 Authorize 없이 바로 호출된다.

## 6. 인증 없이 호출 가능한 API

프론트가 로그인 없이도 지도 화면을 개발할 수 있도록 조회 API는 열려 있다.

| 인증 불필요 | 인증 필요 |
|---|---|
| `GET /api/spots` | 좋아요 등록/취소, 내 좋아요 목록 |
| `GET /api/spots/{id}` | 방문 시작/완료 |
| `GET /api/spots/{id}/reviews` | 리뷰 작성/삭제 |
| `POST /api/auth/google`, `/api/auth/refresh` | 내 정보 조회/수정, 탈퇴 |

전체 스펙은 [api.md](./api.md) 참고.

## 7. 외부에 서버 공개하기 (프론트가 원격일 때)

로컬 서버를 인터넷에서 접근 가능하게 하려면 Cloudflare Tunnel을 쓴다.

```bash
cloudflared tunnel --url http://localhost:8090
```

출력된 `https://....trycloudflare.com` 주소를 프론트에 전달한다. API는 그 뒤에 `/api/...`를 붙여 호출.

- 서버 터미널과 터널 터미널을 **둘 다 켜둬야** 접속이 유지된다.
- 무료 quick tunnel은 재시작할 때마다 URL이 바뀐다.
- 서버만 재시작할 때는 터널을 끄지 않아도 된다(포트만 바라보므로 URL 유지).
