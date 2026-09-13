import 'package:coltrip/core/network/dio_client.dart';
import 'package:coltrip/core/network/token_storage.dart';
import 'package:dio/dio.dart';
import 'package:get/get.dart';

import '../../app/routes/app_routes.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor(this._tokens);

  final TokenStorage _tokens;

  Future<bool>? _refreshing;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final isAuthPath = options.path.startsWith('/api/auth/');

    if (!isAuthPath) {
      final token = await _tokens.readAccessToken();
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final req = err.requestOptions;
    final is401 = err.response?.statusCode == 401;
    final isAuthPath = req.path.startsWith('/api/auth/');
    final alreadyRetried = req.extra['authRetried'] == true;
    if (!is401 || isAuthPath || alreadyRetried) {
      return handler.next(err);
    }

    final pending = _refreshing ??= _refresh();
    final refreshed = await pending;
    if (identical(_refreshing, pending)) _refreshing = null;

    if (!refreshed) {
      await _tokens.clearTokens();
      _redirectToLogin();
      return handler.next(err);
    }
    try {
      final newToken = await _tokens.readAccessToken();
      final clone = await DioClient.plain.request<dynamic>(
        req.path,
        data: req.data,
        queryParameters: req.queryParameters,
        options: Options(
          method: req.method,
          headers: {
            ...req.headers,
            'Authorization': 'Bearer $newToken',
          },
          extra: {...req.extra, 'authRetried': true},
          contentType: req.contentType,
          responseType: req.responseType,
        ),
      );
      return handler.resolve(clone);
    } on DioException catch (e) {
      return handler.next(e);
    }
  }

  /// refreshToken 으로 accessToken/refreshToken 재발급. 성공 시 저장하고 true.
  Future<bool> _refresh() async {
    final refreshToken = await _tokens.readRefreshToken();
    if (refreshToken == null) return false;

    try {
      final res = await DioClient.plain.post<Map<String, dynamic>>(
        '/api/auth/refresh',
        options: Options(
          headers: {'Authorization': 'Bearer $refreshToken'},
        ),
      );

      final data = res.data;
      final access = data?['accessToken'] as String?;
      final refresh = data?['refreshToken'] as String?;
      if (access == null || refresh == null) return false;

      await _tokens.saveTokens(accessToken: access, refreshToken: refresh);
      return true;
    } on DioException {
      // 401 InvalidRefreshTokenException 등 → 재로그인 필요
      return false;
    }
  }

  void _redirectToLogin() {
    if (Get.currentRoute != AppRoutes.login) {
      Get.offAllNamed(AppRoutes.login);
    }
  }
}
