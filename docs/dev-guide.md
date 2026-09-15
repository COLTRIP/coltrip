# 백엔드 개발 환경 가이드

가상 위치 시연은 일반 로컬 실행과 분리한다. [시연 모드 준비·접근 제한](./demo-mode-spec.md)을 참고한다. demo 프로필은 별도 DB/키/허용 계정 설정 없이는 기동하지 않는다.

기준: 2026-09-14, develop 727bf28의 Java 21 toolchain, application.yml, application-secret.example.yml.
아래 PowerShell 명령은 별도 표시가 없으면 **backend 폴더**에서 실행한다.
운영 DB가 아닌 개인 로컬 DB를 사용한다.

## 1. Java와 작업 폴더
저장소 루트에서:
```powershell
Set-Location backend
java -version
.\gradlew.bat --version
```

JDK 21이 필요하다. Gradle은 wrapper로 실행하므로 별도 설치하지 않는다.
PowerShell/cmd는 gradlew.bat, macOS/Linux/Git Bash는 ./gradlew를 사용한다.
Java가 다르면 설치된 JDK 21 경로로 JAVA_HOME과 PATH를 설정한다.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

경로에 공백이 있으면 따옴표로 감싼다. 첫 Gradle 실행은 의존성 다운로드를 위한 인터넷이 필요하다.

## 2. MySQL 준비

### Docker 사용
Docker Desktop을 실행하고 Linux 컨테이너 엔진이 준비될 때까지 기다린다.

```powershell
docker info
docker ps -a
```

dockerDesktopLinuxEngine 파이프를 찾지 못하면 Docker 엔진이 실행되지 않은 것이다. 이 상태에서는 DB 생성 명령을 반복하지 않는다.
coltrip-mysql 컨테이너가 없을 때만 최초 생성한다. 아래 암호는 로컬 예시이며 공유/운영 환경에서 재사용하지 않는다.

```powershell
docker run --name coltrip-mysql -e MYSQL_ROOT_PASSWORD=local-dev-only -e MYSQL_DATABASE=coltrip -p 127.0.0.1:3306:3306 -v coltrip-mysql-data:/var/lib/mysql -d mysql:8.4
docker logs coltrip-mysql
```

ready for connections 이후 접속한다. MYSQL_DATABASE는 빈 데이터 볼륨 최초 초기화에만 적용된다.
기존 컨테이너가 중지 상태라면 재생성하지 않고 다음을 사용한다.

```powershell
docker start coltrip-mysql
```

3306 포트가 이미 사용 중이면 기존 로컬 MySQL을 사용할지 결정한다.
새 컨테이너를 3307로 노출하려면 -p 127.0.0.1:3307:3306으로 생성하고 JDBC URL도 3307로 맞춘다.
기존 볼륨의 비밀번호는 docker run 환경변수 변경만으로 바뀌지 않는다.

### 로컬 MySQL 사용
MySQL 서버 서비스를 시작한 뒤 MySQL Workbench SQL 편집기 또는 mysql 클라이언트에서 접속한다.
CREATE DATABASE는 PowerShell 명령이 아니라 **접속 후 MySQL에서 실행할 SQL**이다.

```powershell
mysql -h 127.0.0.1 -u root -p
```

Docker DB의 SQL 콘솔을 열 때:
```powershell
docker exec -it coltrip-mysql mysql -u root -p
```

SQL 콘솔에서:
```sql
CREATE DATABASE IF NOT EXISTS coltrip CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SHOW DATABASES;
```

## 3. 비밀 설정
backend 폴더에서 아래 명령은 기존 비밀 설정을 덮어쓰지 않는다.

```powershell
if (-not (Test-Path 'src/main/resources/application-secret.yml')) {
    Copy-Item 'src/main/resources/application-secret.example.yml' 'src/main/resources/application-secret.yml'
}
```

application-secret.yml을 열어 datasource URL/계정/암호, 충분히 긴 랜덤 jwt.secret,
Google OAuth 값, internal.api-key를 채운다. 예시 값을 운영 비밀키로 사용하지 않는다.
이 파일은 Git 제외 대상이다. 토큰·비밀키를 문서/PR/로그에 올리지 않는다.

- application.yml은 optional:application-secret.yml을 import한다.
- DB 설정이 없으면 DataSource url/driver 오류가 난다. develop 코드가 최신이어도 제외된 비밀 설정은 git pull로 오지 않는다.
- DB 연결 거부는 서버/포트, Access denied는 계정/암호, Unknown database는 DB 생성을 확인한다.
- 기본 server.port는 **8080**. 예시 secret 파일의 server.port는 **8090**이며 import 후 적용된다.
- 실제 포트는 기동 로그를 확인한다. 아래 URL 예시는 예시 secret 파일을 적용한 8090 기준이다.
- AI 실시간 추천은 AI_BASE_URL, AI_API_KEY, AI_DATA_SOURCE(real 기본값, mock 선택) 설정이 별도로 필요하다.
- AI_API_KEY는 백엔드→AI X-API-Key용, internal.api-key는 AI→백엔드 X-Internal-Api-Key용이다.
- 서버 JVM 시간대도 Asia/Seoul로 맞춘다. 기존 LocalDateTime 기반 관측/방문 응답에는 오프셋이 없다.

로컬에서 AI 호출 없이 기동만 확인하려면 스케줄러를 끌 수 있다. 이는 push API나 수동 대체지 API를 비활성화하는 설정은 아니다.
```powershell
$env:QUIET_INDEX_MAP_SYNC_CRON = '-'
```
다시 기본 주기를 사용하려면 위 환경변수를 제거한 뒤 서버를 재시작한다.
```powershell
Remove-Item Env:QUIET_INDEX_MAP_SYNC_CRON -ErrorAction SilentlyContinue
```
기본값은 매시 5분(`0 5 * * * *`)이다. AI 키/주소를 준비하고 실제 반영 건수는 동기화 로그에서 확인한다.

## 4. 서버 실행·종료
```powershell
.\gradlew.bat bootRun
```

Started BackendApplication 로그와 실제 포트를 확인한다. Gradle 진행률만으로 기동 성공이라고 판단하지 않는다.
8090 충돌 시 일회성으로 다른 포트를 지정할 수 있다.

```powershell
.\gradlew.bat bootRun --args="--server.port=8091"
```

서버 종료는 서버 터미널에서 Ctrl+C. Docker DB도 중지하려면:
```powershell
docker stop coltrip-mysql
```

데이터 볼륨 삭제나 docker system prune은 일반 종료 절차가 아니다.

## 5. 개발 시드
서버가 정상 기동해 테이블이 생성된 후, 실제 데이터가 아닌 로컬 시연 데이터가 필요할 때만 적재한다.
seed/seed-spots.sql에 부산 12곳과 감성모드가 정의되어 있다. 실제 DB에 이미 들어 있는지는 별도로 조회한다.

로컬 mysql 콘솔에서 절대 경로로:
```sql
USE coltrip;
SOURCE C:/path/to/coltrip/backend/seed/seed-spots.sql;
```

Docker DB이면 먼저 파일을 복사하고 SQL 콘솔을 연다.
```powershell
docker cp ./seed/seed-spots.sql coltrip-mysql:/tmp/seed-spots.sql
docker exec -it coltrip-mysql mysql -u root -p coltrip
```

그 SQL 콘솔에서:
```sql
SOURCE /tmp/seed-spots.sql;
```

시드의 SEED- ID를 실 TourAPI contentId로 간주하지 않는다. 실데이터는 [적재 API](./spot-import-api-spec.md)를 참고한다.
시드 삭제 전 방문/좋아요/리뷰/관측/예측/모드/대체지 FK와 제안 JSON 참조를 확인하고 백업·이전 정책을 정한다.
실데이터 적재가 시드 참조를 자동 이전하지 않으며 FK 검사를 끄거나 일괄 DELETE하지 않는다.

## 6. 인증 테스트
실제 Google 로그인은 플랫폼 SDK 및 OAuth 설정이 준비되어 있어야 한다. 현재 팀 등록 상태는 별도 확인한다.
로컬 테스트 계정 스크립트 seed/create-test-user.sh는 Bash, mysql 클라이언트, python3/venv/pip와 로컬 secret 설정이 필요하다.
Git Bash에서 저장소 루트 기준으로 실행하며 PowerShell 스크립트가 아니다. Docker CLI만 설치된 환경에서는 mysql 클라이언트가 추가로 필요하다.
출력된 토큰은 비밀 정보이며 공유 저장소에 커밋하지 않는다.

PowerShell에서 토큰 재발급:
```powershell
$baseUrl = 'http://localhost:8090'
$refreshToken = Read-Host '로컬 테스트 리프레시 토큰'
$tokens = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/refresh" -Headers @{Authorization="Bearer $refreshToken"}
$refreshToken = $tokens.refreshToken
Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Headers @{Authorization="Bearer $($tokens.accessToken)"}
```

재발급하면 이전 refreshToken은 무효화된다. 다음에는 새 refreshToken을 사용한다.
같은 리프레시 토큰으로 동시에 호출하면 한 요청만 성공하고 나머지는 401이다. 프론트에서는 재발급 요청을 하나로 합치고 응답의 두 토큰을 함께 교체한다.
Windows PowerShell의 curl 별칭 문제를 피하려면 Invoke-RestMethod 또는 curl.exe를 명시한다.

## 7. Swagger 및 공개 API
브라우저에서 http://localhost:8090/swagger-ui/index.html 에 접속한다.
OpenAPI JSON은 /v3/api-docs 이다. 서버가 8080/8091이면 URL도 같은 포트로 바꾼다.

- 모든 GET /api/spots/**는 비로그인 허용(추천/대체지/관측·예측 타임라인 포함).
- /api/auth/google은 idToken 본문, /api/auth/refresh는 refreshToken 헤더가 필요하다.
- 방문/내 정보/쓰기 API는 액세스 토큰이 필요하다.
- Swagger Authorize에는 Bearer 접두어 없이 토큰만 넣는다. refresh 호출 시 리프레시 토큰을 넣고, 이후 액세스 토큰으로 교체한다.
- 내부 API는 사용자 토큰이 아니라 X-Internal-Api-Key 요청 헤더를 사용한다.
- Swagger의 보안 표시와 실행 시 보안 처리는 다를 수 있으므로 [API 인증 정책](./api.md)을 기준으로 확인한다.

## 8. 원격 터널
서버가 로컬에서 정상 응답하는지 확인한 후 **새 터미널**에서:
```powershell
cloudflared tunnel --url http://localhost:8090
```

--url은 ASCII 하이픈 두 개다. 문서에서 복사한 긴 대시(—)를 사용하지 않는다.
출력된 HTTPS 주소 뒤에 /swagger-ui/index.html 또는 /api/...를 붙인다.
서버와 터널 터미널을 모두 유지해야 하며 quick tunnel 재시작 시 주소가 달라질 수 있다.
터널 종료는 해당 터미널에서 Ctrl+C. 이는 고정 도메인 배포를 대신하지 않는다.
터널은 로컬 서버를 외부에 공개하므로 테스트용 데이터만 사용하고 필요할 때만 켠다.

## 9. 테스트와 배포 전 확인
```powershell
.\gradlew.bat test
.\gradlew.bat build
```

기존 전체 테스트에는 SpringBootTest 및 DB 쓰기 테스트가 있다. 운영 DB를 연결한 상태로 실행하지 않는다.
CI는 별도 MySQL 서비스와 테스트용 환경변수로 실행한다. H2 통과가 실제 MySQL 동시성 검증을 대신하지 않는다.
docs만 변경한 PR은 현재 CI paths 필터상 backend CI가 실행되지 않을 수 있다.

문서 대조 시에는 공개 장소 GET의 익명 응답, 현재 방문 없음의 `200 {"visit":null}`, 회원 응답의 `currentVisitId`, 대체지의 `score` 및 빈 배열/message를 우선 확인한다. 정상 빈 목록과 502/503/504 장애는 구분한다. Swagger에 선언되지 않은 오류도 있으므로 [대조 기록](./documentation-audit.md)과 [공통 오류](./api.md)를 함께 확인한다.

관광지 적재 계약, 관측값 공급 주기, 날짜별 예측 계약 및 AI 실데이터 연결은 [task.md](./task.md)에서 별도 관리한다.
