# 개발 환경 가이드 (백엔드)

로컬에서 서버를 띄우고, 프론트가 API를 테스트할 수 있게 하는 방법.

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

포트가 이미 사용 중이면 다른 포트로:
```bash
./gradlew bootRun --args='--server.port=8090'
```

> 여러 프로젝트를 동시에 돌린다면 `application-secret.yml`에 `server.port`를 넣어 개인 포트를 고정할 수도 있다.
> 단, `application.yml`에 이미 `server.port`가 있으면 그쪽이 우선하므로 실행 인자로 넘기는 편이 확실하다.

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

## 5. 인증 없이 호출 가능한 API

프론트가 로그인 없이도 지도 화면을 개발할 수 있도록 조회 API는 열려 있다.

| 인증 불필요 | 인증 필요 |
|---|---|
| `GET /api/spots` | 좋아요 등록/취소, 내 좋아요 목록 |
| `GET /api/spots/{id}` | 방문 시작/완료 |
| `GET /api/spots/{id}/reviews` | 리뷰 작성/삭제 |
| `POST /api/auth/google`, `/api/auth/refresh` | 내 정보 조회/수정, 탈퇴 |

전체 스펙은 [api.md](./api.md) 참고.

## 6. 외부에 서버 공개하기 (프론트가 원격일 때)

로컬 서버를 인터넷에서 접근 가능하게 하려면 Cloudflare Tunnel을 쓴다.

```bash
cloudflared tunnel --url http://localhost:8090
```

출력된 `https://....trycloudflare.com` 주소를 프론트에 전달한다. API는 그 뒤에 `/api/...`를 붙여 호출.

- 서버 터미널과 터널 터미널을 **둘 다 켜둬야** 접속이 유지된다.
- 무료 quick tunnel은 재시작할 때마다 URL이 바뀐다.
- 서버만 재시작할 때는 터널을 끄지 않아도 된다(포트만 바라보므로 URL 유지).
