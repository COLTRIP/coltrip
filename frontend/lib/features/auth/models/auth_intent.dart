enum AuthIntent {
  login,
  signup;

  String get apiValue => switch (this) {
    AuthIntent.login => 'LOGIN',
    AuthIntent.signup => 'SIGNUP',
  };
}