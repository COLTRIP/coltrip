import 'package:dio/dio.dart';

import '../storage/token_storage.dart';

class DioClient {
  DioClient._();

  static const TokenStorage _tokenStorage = TokenStorage();

  static final Dio instance = _createDio();

  static final Dio plain = Dio(
    BaseOptions(
      baseUrl: 'https://api.coltrip.co.kr',
      connectTimeout: const Duration(seconds: 10),
      sendTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),
      contentType: Headers.jsonContentType,
      responseType: ResponseType.json,
    ),
  );

  static Dio _createDio() {
    final dio = Dio(
      BaseOptions(
        baseUrl: 'https://api.coltrip.co.kr',
        connectTimeout: const Duration(seconds: 10),
        sendTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
        contentType: Headers.jsonContentType,
        responseType: ResponseType.json,
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final accessToken = await _tokenStorage.readAccessToken();

          if (accessToken != null && accessToken.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $accessToken';
          }

          handler.next(options);
        },
      ),
    );

    return dio;
  }
}
