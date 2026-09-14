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
    final id = json['id'];
    final email = json['email'];

    if (id is! num) {
      throw const FormatException('사용자 ID가 없거나 올바르지 않습니다.');
    }
    if (email is! String || email.isEmpty) {
      throw const FormatException('이메일이 없거나 올바르지 않습니다.');
    }

    return AuthUser(
      id: id.toInt(),
      email: email,
      nickname: json['nickname'] as String?,
    );
  }
}
