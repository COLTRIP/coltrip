#!/usr/bin/env bash
# 개발용 테스트 계정 생성 + 리프레시 토큰 발급 스크립트
#
# Google OAuth Android/iOS 클라이언트 등록이 끝나기 전까지, 프론트가 인증이 필요한 API를
# 테스트할 수 있도록 테스트 계정과 리프레시 토큰을 만들어준다.
#
# 사용법:
#   ./backend/seed/create-test-user.sh [닉네임]
#
# 필요:
#   - 로컬 MySQL에 coltrip DB 및 스키마 생성 완료(서버 1회 이상 기동)
#   - backend/src/main/resources/application-secret.yml (jwt.secret, datasource 설정)
#   - python3
#
# ⚠️ 출력된 토큰은 자격 증명이다. 레포에 커밋하지 말 것.

set -euo pipefail

NICKNAME="${1:-프론트테스터}"
GOOGLE_SUB="frontend-test-user"
EMAIL="frontend-test@coltrip.dev"
TOKEN_VALID_DAYS=90

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SECRET_FILE="$REPO_ROOT/backend/src/main/resources/application-secret.yml"

if [[ ! -f "$SECRET_FILE" ]]; then
  echo "application-secret.yml이 없습니다: $SECRET_FILE" >&2
  exit 1
fi

# application-secret.yml에서 필요한 값 추출
JWT_SECRET=$(grep -A3 '^jwt:' "$SECRET_FILE" | grep 'secret:' | head -1 | sed 's/.*secret: *//')
DB_USER=$(grep 'username:' "$SECRET_FILE" | head -1 | sed 's/.*username: *//')
DB_PASS=$(grep 'password:' "$SECRET_FILE" | head -1 | sed 's/.*password: *//')
DB_URL=$(grep 'url:' "$SECRET_FILE" | head -1 | sed 's/.*url: *//')
DB_NAME="${DB_URL##*/}"
DB_NAME="${DB_NAME%%\?*}"

MYSQL_BIN="$(command -v mysql || echo /usr/local/mysql/bin/mysql)"
if [[ ! -x "$MYSQL_BIN" ]]; then
  echo "mysql 클라이언트를 찾을 수 없습니다." >&2
  exit 1
fi

mysql_exec() {
  "$MYSQL_BIN" -h 127.0.0.1 -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -B -e "$1" 2>/dev/null
}

# 이미 있으면 재사용, 없으면 생성
mysql_exec "INSERT IGNORE INTO user (google_sub, email, nickname, role, created_at, updated_at)
            VALUES ('$GOOGLE_SUB', '$EMAIL', '$NICKNAME', 'USER', NOW(), NOW());" >/dev/null
USER_ID=$(mysql_exec "SELECT id FROM user WHERE google_sub='$GOOGLE_SUB';")

if [[ -z "$USER_ID" ]]; then
  echo "테스트 계정 생성에 실패했습니다. DB 연결 정보를 확인해주세요." >&2
  exit 1
fi

VENV_DIR="$(mktemp -d)"
trap 'rm -rf "$VENV_DIR"' EXIT
python3 -m venv "$VENV_DIR" >/dev/null 2>&1
"$VENV_DIR/bin/pip" install --quiet pyjwt >/dev/null 2>&1

REFRESH_TOKEN=$("$VENV_DIR/bin/python3" - "$USER_ID" "$JWT_SECRET" "$TOKEN_VALID_DAYS" <<'PY'
import sys, jwt, datetime
user_id, secret, days = sys.argv[1], sys.argv[2], int(sys.argv[3])
now = datetime.datetime.now(datetime.timezone.utc)
print(jwt.encode(
    {"sub": user_id, "type": "REFRESH", "iat": now,
     "exp": now + datetime.timedelta(days=days)},
    secret, algorithm="HS256"))
PY
)

# 서버는 DB에 저장된 값과 일치하는 리프레시 토큰만 인정한다
mysql_exec "UPDATE user SET refresh_token='$REFRESH_TOKEN' WHERE id=$USER_ID;" >/dev/null

cat <<EOF

테스트 계정 준비 완료
  user id : $USER_ID
  nickname: $NICKNAME
  유효기간 : ${TOKEN_VALID_DAYS}일

리프레시 토큰:
$REFRESH_TOKEN

사용법:
  curl -X POST http://localhost:8090/api/auth/refresh \\
    -H "Authorization: Bearer <위 리프레시 토큰>"

  응답의 accessToken을 이후 요청의 Authorization 헤더에 사용.

⚠️ 리프레시 토큰은 1회용입니다. /api/auth/refresh를 호출하면 새 리프레시 토큰이
   함께 발급되고 이전 토큰은 즉시 무효화되므로, 응답에 담긴 refreshToken을
   반드시 저장해서 다음 재발급에 사용해야 합니다.
   토큰이 꼬이면 이 스크립트를 다시 실행하면 됩니다.
EOF
