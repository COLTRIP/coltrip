# 부산 가상 위치 시연 모드

## 범위와 최신 방문 계약

이 기능은 관리자 권한이나 GPS 위조 API가 아니다. 프론트의 로고 5회 클릭은 시연 UI 진입 동작이고, 서버는 검증된 Google 계정 허용 목록 또는 시연 전용 게스트 계정으로 접근을 제한한다. 클라이언트가 보내는 demo=true, X-Demo 등의 값으로 인증/권한/방문 검증을 완화하지 않는다.

심사위원 등 사전에 Google 계정을 받기 어려운 대상을 위해 **게스트 로그인**(2026-09-16 추가)을 지원한다. `POST /api/demo/guest-session`을 호출하면 Google 인증 없이 즉시 새 게스트 계정(googleSub가 `demo-guest-` 접두사 + UUID)을 만들고 토큰을 발급한다. 호출마다 별도 계정이 생기므로 여러 명이 동시에 써도 서로 방문 상태가 섞이지 않는다. 이 엔드포인트는 demo.enabled=true에서만 빈이 등록되며 운영 환경에는 라우트 자체가 없다(404). 기존 Google+허용 목록 로그인도 그대로 쓸 수 있다 — 팀 내부 계정으로 시연 서버를 확인하고 싶을 때 병행 가능.

이 브랜치에는 #113이 반영되어 **방문 시작은 spotId만 받고, 완료는 본문 없이 호출**한다. 서버는 방문 좌표를 받거나 반경을 판정하지 않는다. 반경 내/외 판정은 프론트가 가상 위치와 visitRadiusMeters로 수행한다. 과거 이슈의 '서버 반경 검증 유지/반경 밖 거부 테스트'는 현재 계약에 맞지 않아 백엔드 완료 항목으로 표시하지 않는다. 이번 변경에서 해당 팀원 작업을 되돌리지 않는다.

기존 본인 방문 확인, STARTED 상태, 중복 시작 방지, 로그인/토큰 검증은 그대로 유지한다.

## 보안 경계

- 기본 demo.enabled=false. 기존 일반 환경의 공개 관광지 GET 정책은 그대로다.
- 시연 실행에는 **demo 단독 프로필**, demo.enabled=true, app.environment=demo가 모두 필요하다. 다른 프로필과 혼합하거나 demo 프로필에서 enabled=false로 우회하면 기동 실패한다.
- 허용된 Google sub를 demo.allowed-google-subs에 등록한다. 이메일/닉네임/클라이언트가 보낸 사용자 ID로 판정하지 않는다. 비어 있는 목록과 와일드카드는 기동 실패한다.
- 로그인/가입: 기존 Google idToken 검증 후 허용 목록을 검사한다. 불허 가입은 사용자/토큰을 생성하지 않는다.
- 재발급: 검증된 토큰의 DB 사용자 Google sub를 검사한다. 불허 계정에는 새 토큰을 발급하지 않는다.
- 로그인/재발급/게스트 세션 발급 POST 이외 요청은 기존 액세스 JWT와 DB 사용자 허용 목록(또는 게스트 계정) 검사를 모두 통과해야 한다. 공개 장소 조회도 시연 환경에서는 인증이 필요하다. 삭제된 계정과 허용 목록에서 제외된 계정은 접근하지 못한다.
- 게스트 계정은 googleSub 접두사(`demo-guest-`)로만 식별한다. 이 접두사를 가진 계정은 허용 목록 등록 없이도 통과하며, 그 외 계정은 기존과 동일하게 허용 목록 검사를 받는다.
- 내부 /api/internal/**는 시연 환경에서 차단한다. 시연 데이터는 별도 DB에 승인된 시드/관광지 데이터만 적재한다.
- Swagger와 API 문서도 익명 공개하지 않는다. 일반 브라우저 탐색에는 Bearer 헤더가 없으므로 시연 서버 Swagger UI 접근이 제한된다. 호출 검증은 인증 헤더를 넣는 API 도구를 사용한다.
- FCM 빈, 백그라운드 방문 평가, AI 지도 자동 동기화 빈은 시연 활성 시 등록하지 않는다. FCM 자격증명이 지정되면 기동도 거부한다. 테스트 기기에도 푸시를 보내지 않는 정책이다.
- 사용자 선택으로 직접 호출하는 대체지 API는 기존 AI 설정을 사용한다. 필요할 때만 승인된 테스트 AI 환경을 설정한다.

새 관리자 역할이나 인증/방문 검증 건너뛰기 API는 없다. 게스트 세션 발급은 인증을 건너뛰는 게 아니라 별도 계정을 새로 만드는 것이며, 그 계정으로도 기존 방문 소유권/상태 검증을 동일하게 받는다. 허용 목록 변경 후 서버를 재시작한다.

## 운영 데이터 격리

기동 검증은 DataSource/JPA 생성 **이전**에 실행된다. 지정한 demo.datasource-*와 실제 spring.datasource.*가 다르면 실패한다. MySQL URL은 DB 이름 coltrip_demo만 허용하며 테스트용 H2는 coltrip_demo 이름의 인메모리 DB만 허용한다. JWT는 32자 이상의 demo.jwt-secret을 명시하고 실제 jwt.secret이 그 값과 일치해야 한다.

이 검증만으로 호스트의 소유권, DB 계정 권한, 운영 키와의 중복까지 자동 증명할 수는 없다. 다음은 운영 담당자가 반드시 보장해야 한다.

1. 별도 테스트 DB/서버와 coltrip_demo 전용 권한 계정 사용. 운영 coltrip DB 권한 부여 금지.
2. 운영과 다른 JWT 키 사용. 테스트 DB에 운영 사용자/방문/리뷰/좋아요/기기 토큰 백업을 복원하지 않기.
3. 테스트용 Google OAuth 설정과 AI 환경을 분리하고 운영 서버의 환경변수를 그대로 복사하지 않기.
4. 테스트 빌드의 API 주소와 토큰 저장소를 운영 빌드와 분리. 모드 전환 시 이전 세션을 재사용하지 않기.
5. 공개 배포 전 가능하면 네트워크/VPN 접근 제한도 적용.

시연 기록에는 별도 demo 플래그를 붙이지 않는다. 별도 DB 안에서 실제 흐름대로 방문/좋아요/리뷰를 생성한다. 운영 통계에는 이 DB를 연결하지 않는다.

## 로컬 준비 예시 (PowerShell)

아래 작업은 운영 서버가 아닌 개발 PC의 새 터미널에서 수행한다. Docker Desktop Linux 엔진을 먼저 실행한다. 예시 비밀번호는 로컬 전용이며 외부 공개 서버에서 재사용하지 않는다.

```powershell
docker run --name coltrip-demo-mysql -e MYSQL_ROOT_PASSWORD=local-demo-root-only -e MYSQL_DATABASE=coltrip_demo -e MYSQL_USER=coltrip_demo_user -e MYSQL_PASSWORD=local-demo-only -p 127.0.0.1:3307:3306 -v coltrip-demo-mysql-data:/var/lib/mysql -d mysql:8.4
docker logs coltrip-demo-mysql
```

기존 동명 컨테이너가 있으면 재생성하지 않고 설정을 확인한다. 기존 데이터 볼륨에는 환경변수 변경으로 계정/암호가 다시 적용되지 않는다.

backend 폴더에서 시연 전용 환경변수를 지정한다.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:APP_ENVIRONMENT = 'demo'
$env:DEMO_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3307/coltrip_demo'
$env:DEMO_DATASOURCE_USERNAME = 'coltrip_demo_user'
$env:DEMO_DATASOURCE_PASSWORD = 'local-demo-only'
$env:DEMO_ALLOWED_GOOGLE_SUBS = Read-Host '허용할 검증된 Google sub (여러 개는 쉼표 구분)'
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$keyBytes = New-Object byte[] 32
$rng.GetBytes($keyBytes)
$env:DEMO_JWT_SECRET = [Convert]::ToBase64String($keyBytes)
$rng.Dispose()
Remove-Item Env:FCM_CREDENTIALS_PATH -ErrorAction SilentlyContinue
```

Google sub는 검증한 Google 계정 식별자다. ID 토큰이나 비밀키를 Git/공개 이슈에 붙이지 않는다. 환경마다 사용자/토큰 DB를 분리하므로 허용된 계정도 시연 DB에서 기존 SIGNUP 흐름으로 먼저 가입한다.

처음 생성한 **빈 coltrip_demo DB에만**, 스키마 초기화 후 정상 기동을 확인한다.

```powershell
.\gradlew.bat bootRun --args="--server.port=8091 --spring.jpa.hibernate.ddl-auto=update"
```

기본 demo 프로필은 ddl-auto=validate다. 최초 초기화를 마치고 Ctrl+C로 종료한 후 이후에는 다음처럼 실행한다.

```powershell
.\gradlew.bat bootRun --args="--server.port=8091"
```

실제 포트와 테스트 DB를 확인한 다음 관광지 시드만 승인된 방식으로 적재한다. 키와 DB 설정은 저장소에 커밋하지 않는다. 실제 테스트 서버 생성/배포/계정 등록은 이번 코드 작업에서 자동 수행하지 않는다.

## 프론트 계약과 테스트

1. 테스트 빌드에서 로고 5회 클릭, 시연 모드 표시. 운영 빌드 진입 동작만으로 서버 접근권한이 생기지 않음.
2. 로그인 방법은 둘 중 하나. (a) 심사위원 등 사전 계정 등록이 어려운 대상: POST /api/demo/guest-session 호출해 Google 인증 없이 바로 토큰 발급받음. (b) 팀 내부 확인용: 허용 Google 계정으로 기존처럼 가입/로그인. API별 기존 Bearer 액세스 토큰 사용은 동일.
3. 기본 가상 좌표 예시: 부산 시청 35.1796, 129.0756. 프론트의 지도 중심과 거리/반경 계산에 사용한다. 현재 추천 API는 사용자 좌표를 받지 않고 필터에 맞는 장소를 고요지수 순으로 반환한다.
4. 방문 시작 POST /api/visits/start: {"spotId": ...}. 좌표는 전송하지 않음.
5. 가상 위치를 목적지 반경 안으로 이동한 뒤 프론트가 판정하고 PATCH /api/visits/{id}/complete 호출. 반경 밖이면 프론트가 완료 요청을 보내지 않음.
6. 완료 후 재완료 409, 다른 사용자의 방문 완료/취소 404가 유지되는지 확인.
7. 종료 시 가상 위치 사용 중단, 실제 위치 모드 복귀 및 테스트 토큰/서버 설정 혼용 방지.

서버 응답: 익명 요청 401 UnauthorizedException, 유효한 토큰이지만 불허 계정 또는 내부 API 요청은 403 DemoAccessDenied. 로그인/재발급의 기존 잘못된 토큰 401 정책은 유지한다.

DemoConfigurationTest는 환경/DB/키/허용 목록 및 초기화 이전 차단을 검증하고 DemoModeIntegrationTest는 계정 제한, 발급 차단, 게스트 세션 발급과 즉시 사용, 부산 추천과 방문 흐름, 소유권/상태, 푸시/자동 배치 비활성화를 검증한다. DemoDisabledIntegrationTest는 운영 프로필에 게스트 세션 엔드포인트 자체가 없음(404)을 확인한다. H2 테스트는 MySQL 권한 격리나 실제 기기의 가상 GPS/반경 판정 검증을 대신하지 않는다.
