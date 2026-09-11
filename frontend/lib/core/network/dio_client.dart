import 'package:coltrip/core/network/token_storage.dart';
import 'package:dio/dio.dart';

import 'auth_interceptor.dart';

class DioClient {
  DioClient._();

  static const baseUrl = 'https://organ-aimed-saying-revised.trycloudflare.com';

  static BaseOptions _baseOptions() => BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 10),
        sendTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
        contentType: Headers.jsonContentType,
        responseType: ResponseType.json,
      );

  /// 인증 인터셉터가 붙은 기본 클라이언트
  static final Dio instance = _create();

  /// 인터셉터 없는 클라이언트. 토큰 재발급 / 401 후 원요청 재시도 전용
  /// (AuthInterceptor 가 자기 자신을 다시 타면서 무한 루프 도는 것 방지)
  static final Dio plain = Dio(_baseOptions());

  static Dio _create() {
    final dio = Dio(_baseOptions());
    dio.interceptors.add(AuthInterceptor(const TokenStorage()));
    return dio;
  }
}
