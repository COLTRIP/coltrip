import 'auth_user.dart';

class AuthResponse {
  const AuthResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.isNewUser,
    required this.user,
  });

  final String accessToken;
  final String refreshToken;
  final bool isNewUser;
  final AuthUser user;

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    final accessToken = json['accessToken'];
    final refreshToken = json['refreshToken'];
    final user = json['user'];

    if (accessToken is! String || accessToken.isEmpty) {
      throw const FormatException('accessToken이 없거나 올바르지 않습니다.');
    }
    if (refreshToken is! String || refreshToken.isEmpty) {
      throw const FormatException('refreshToken이 없거나 올바르지 않습니다.');
    }
    if (user is! Map<String, dynamic>) {
      throw const FormatException('사용자 정보가 없거나 올바르지 않습니다.');
    }

    return AuthResponse(
      accessToken: accessToken,
      refreshToken: refreshToken,
      isNewUser: json['isNewUser'] as bool? ?? false,
      user: AuthUser.fromJson(user),
    );
  }
}
