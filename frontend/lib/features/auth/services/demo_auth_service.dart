import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/storage/api_environment_storage.dart';
import '../../../core/storage/token_storage.dart';

/// 시연 서버의 게스트 세션을 발급하고 앱의 API 환경을 전환합니다.
class DemoAuthService {
  DemoAuthService({
    Dio? dio,
    TokenStorage? tokenStorage,
    ApiEnvironmentStorage? environmentStorage,
  }) : _dio = dio ?? DioClient.plain,
       _tokenStorage = tokenStorage ?? const TokenStorage(),
       _environmentStorage =
           environmentStorage ?? const ApiEnvironmentStorage();

  final Dio _dio;
  final TokenStorage _tokenStorage;
  final ApiEnvironmentStorage _environmentStorage;

  Future<void> enableDemoMode() async {
    await _tokenStorage.clearTokens();
    await _environmentStorage.setDemoMode(true);
    DioClient.configureEnvironment(demoMode: true);
  }

  Future<void> startGuestSession() async {
    // 운영 토큰과 시연 토큰이 섞이지 않도록 서버 전환 전에 기존 세션을 비웁니다.
    await _tokenStorage.clearTokens();
    DioClient.configureEnvironment(demoMode: true);

    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/demo/guest-session',
      );
      final data = response.data;
      final accessToken = data?['accessToken'];
      final refreshToken = data?['refreshToken'];

      if (accessToken is! String || accessToken.isEmpty) {
        throw const ApiException('시연용 access token이 올바르지 않습니다.');
      }
      if (refreshToken is! String || refreshToken.isEmpty) {
        throw const ApiException('시연용 refresh token이 올바르지 않습니다.');
      }

      await _tokenStorage.saveTokens(
        accessToken: accessToken,
        refreshToken: refreshToken,
      );
      await _environmentStorage.setDemoMode(true);
    } on DioException catch (error) {
      await _restoreProductionEnvironment();
      throw ApiException.fromDioException(error);
    } catch (_) {
      await _restoreProductionEnvironment();
      rethrow;
    }
  }

  Future<void> exitDemoMode() async {
    await _tokenStorage.clearTokens();
    await _environmentStorage.setDemoMode(false);
    DioClient.configureEnvironment(demoMode: false);
  }

  Future<void> _restoreProductionEnvironment() async {
    await _tokenStorage.clearTokens();
    await _environmentStorage.setDemoMode(false);
    DioClient.configureEnvironment(demoMode: false);
  }
}
