class AuthUser {
  const AuthUser({
    required this.id,
    required this.email,
    required this.nickname,
  });

  final int id;
  final String email;
  final String? nickname;

  factory AuthUser.fromJson(Map<String, dynamic> json) {
    return AuthUser(
      id: json['id'] as int,
      email: json['email'] as String,
      nickname: json['nickname'] as String?,
    );
  }
}