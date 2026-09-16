/// 인증 요청의 목적을 나타내는 열거형입니다.
///
/// 로그인과 회원가입을 구분하며, [apiValue]를 통해 서버 API에서 사용하는 값으로 변환합니다.
enum AuthIntent {
  login,
  signup;

  String get apiValue => switch (this) {
    AuthIntent.login => 'LOGIN',
    AuthIntent.signup => 'SIGNUP',
  };
}
