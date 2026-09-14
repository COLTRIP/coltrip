import 'package:dio/dio.dart';
import 'package:get/get.dart';

import '../../app/routes/app_routes.dart';
import '../storage/token_storage.dart';

/// 인증이 필요한 요청에 access token을 추가하고 401 응답을 처리합니다.
///
/// 동시에 여러 요청이 401을 반환하더라도 토큰 재발급은 한 번만 수행하며,
/// 재발급 성공 시 기존 요청을 한 차례 다시 시도합니다.
class AuthInterceptor extends Interceptor {
  AuthInterceptor(this._tokens, this._plainDio);

  final TokenStorage _tokens;
  final Dio _plainDio;

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
      final clone = await _plainDio.request<dynamic>(
        req.path,
        data: req.data,
        queryParameters: req.queryParameters,
        options: Options(
          method: req.method,
          headers: {...req.headers, 'Authorization': 'Bearer $newToken'},
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

  /// Refresh token으로 토큰을 재발급하고 성공 여부를 반환합니다.
  Future<bool> _refresh() async {
    final refreshToken = await _tokens.readRefreshToken();
    if (refreshToken == null) return false;

    try {
      final res = await _plainDio.post<Map<String, dynamic>>(
        '/api/auth/refresh',
        options: Options(headers: {'Authorization': 'Bearer $refreshToken'}),
      );

      final data = res.data;
      final access = data?['accessToken'] as String?;
      final refresh = data?['refreshToken'] as String?;
      if (access == null || refresh == null) return false;

      await _tokens.saveTokens(accessToken: access, refreshToken: refresh);
      return true;
    } on DioException {
      return false;
    }
  }

  void _redirectToLogin() {
    if (Get.currentRoute != AppRoutes.login) {
      Get.offAllNamed(AppRoutes.login);
    }
  }
}
