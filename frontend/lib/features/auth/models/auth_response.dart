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
    return AuthResponse(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      isNewUser: json['isNewUser'] as bool? ?? false,
      user: AuthUser.fromJson(
        json['user'] as Map<String, dynamic>,
      ),
    );
  }
}