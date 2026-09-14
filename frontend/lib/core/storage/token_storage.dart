import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 로그인 토큰을 플랫폼 보안 저장소에 저장하고 조회합니다.
///
/// access token과 refresh token은 항상 한 쌍으로 저장하거나 삭제합니다.
class TokenStorage {
  const TokenStorage();

  static const _storage = FlutterSecureStorage();

  static const _accessTokenKey = 'accessToken';
  static const _refreshTokenKey = 'refreshToken';
  static const _legacyAccessTokenKey = 'access_token';
  static const _legacyRefreshTokenKey = 'refresh_token';

  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    await Future.wait([
      _storage.write(key: _accessTokenKey, value: accessToken),
      _storage.write(key: _refreshTokenKey, value: refreshToken),
      _storage.delete(key: _legacyAccessTokenKey),
      _storage.delete(key: _legacyRefreshTokenKey),
    ]);
  }

  Future<String?> readAccessToken() {
    return _storage.read(key: _accessTokenKey);
  }

  Future<String?> readRefreshToken() {
    return _storage.read(key: _refreshTokenKey);
  }

  Future<void> clearTokens() async {
    await Future.wait([
      _storage.delete(key: _accessTokenKey),
      _storage.delete(key: _refreshTokenKey),
      _storage.delete(key: _legacyAccessTokenKey),
      _storage.delete(key: _legacyRefreshTokenKey),
    ]);
  }
}
