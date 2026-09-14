# 구글 동시 회원가입 (#98)

## 응답 계약

POST /api/auth/google, intent=SIGNUP의 기존 계정 가입과 동일 계정 동시 가입 충돌은 409 AlreadyRegisteredUserException이다. 기존 공통 code/message 형식을 사용하고 오류 응답에는 토큰을 포함하지 않는다. 단순 동시 가입 시 하나는 200으로 성공하고 다른 요청은 409로 종료한다.

## 처리 경계

- Google 검증 후 SignupTransaction.register의 REQUIRES_NEW 트랜잭션에서 존재 확인, 사용자 생성, 토큰 저장, 응답 생성을 수행한다.
- 사용자 생성은 saveAndFlush로 처리한다. 같은 Google 계정의 중복 INSERT는 DB unique 제약이 막는다.
- 사용자 생성 이후 응답 생성/토큰 저장이 실패해도 해당 가입 전체가 롤백된다. 실패한 가입에서 생성한 토큰을 클라이언트에 반환하지 않는다.
- AuthService는 register 프록시의 롤백이 끝난 뒤 DataIntegrityViolationException을 판정한다.
- MySQL duplicate key(1062/23000) 또는 테스트 DB의 unique violation(23505)이면서, 새 읽기 트랜잭션에서 같은 googleSub가 존재하는 경우만 409로 변환한다.
- 별도 읽기 트랜잭션은 실패한 트랜잭션의 rollback-only 상태와 MySQL REPEATABLE READ의 이전 스냅샷 재사용을 피한다.
- NOT NULL/FK/길이 오류, 다른 Google 계정과의 이메일 충돌 등은 이 경로에서 가입 중복으로 바꾸지 않는다. 기존 전역 오류 정책을 따른다.
- 충돌 요청은 먼저 가입한 사용자의 토큰을 새로 발급하거나 덮어쓰지 않는다.
- 신규 가입 트랜잭션이 커밋된 뒤 통신이 끊기면 재가입은 409이며, 사용자는 LOGIN으로 진행한다. 통신 성공까지 DB 트랜잭션으로 보장하지 않는다.

새 테이블/컬럼/제약 이름 변경은 없다. 기존 google_sub/email unique 제약을 유지한다. 일반 로그인, refresh/logout의 사용자 행 잠금 정책은 유지한다.

## 검증

- SignupConcurrencyTest: 두 최초 존재 확인의 실행 순서를 맞춘 뒤 실제 서비스/DB로 200/409, 단일 사용자, 유효 토큰 확인.
- 재가입 시 409 및 기존 refreshToken 유지.
- 사용자 생성 뒤 응답 생성 실패 시 계정 롤백 및 토큰 미반환.
- 관련 없는 NOT NULL 위반은 500으로 유지.
- AuthServiceTest: MySQL 중복 코드 분류, 동일 Google 계정 확인, 다른 무결성 오류 미변환.
- 테스트에서 Google 인증만 대역으로 처리하고 실제 외부 Google 요청은 보내지 않는다.
- TEST_REQUIRE_MYSQL=true인 기존 PR CI에서는 DB 제품명이 MySQL인지 검사한다. 로컬 H2 성공은 실제 MySQL 검증을 대체하지 않으며 PR CI 결과를 별도로 확인한다.
