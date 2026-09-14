import 'package:dio/dio.dart';

import '../storage/token_storage.dart';
import 'auth_interceptor.dart';

/// COLTRIP 백엔드 통신에 사용하는 Dio 인스턴스를 제공합니다.
///
/// [instance]는 저장된 access token을 요청 헤더에 추가합니다.
/// [plain]은 인증 처리 없이 토큰 재발급 같은 내부 요청에 사용합니다.
class DioClient {
  DioClient._();

  static const TokenStorage _tokenStorage = TokenStorage();

  /// 인증 인터셉터를 거치지 않는 토큰 재발급용 클라이언트입니다.
  static final Dio plain = Dio(_createBaseOptions());

  /// 일반 API 요청에 사용하는 인증 클라이언트입니다.
  static final Dio instance = _createDio();

  static BaseOptions _createBaseOptions() {
    return BaseOptions(
      baseUrl: 'https://api.coltrip.co.kr',
      connectTimeout: const Duration(seconds: 10),
      sendTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),
      contentType: Headers.jsonContentType,
      responseType: ResponseType.json,
    );
  }

  static Dio _createDio() {
    final dio = Dio(_createBaseOptions());

    dio.interceptors.add(AuthInterceptor(_tokenStorage, plain));

    return dio;
  }
}
