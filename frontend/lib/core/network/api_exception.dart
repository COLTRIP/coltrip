import 'package:dio/dio.dart';

class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final String? code;

  const ApiException(this.message, {this.statusCode, this.code});

  factory ApiException.fromDioException(DioException e) {
    // 네트워크 계열 (타임아웃 / 연결 실패)
    if (e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.sendTimeout ||
        e.type == DioExceptionType.connectionError) {
      return const ApiException('네트워크 연결을 확인해주세요.', code: 'NETWORK');
    }

    final data = e.response?.data;
    final serverCode = data is Map ? data['code'] as String? : null;
    final serverMessage = data is Map ? data['message'] as String? : null;

    return ApiException(
      serverMessage ?? e.message ?? '알 수 없는 오류가 발생했습니다.',
      statusCode: e.response?.statusCode,
      code: serverCode,
    );
  }

  @override
  String toString() => message;
}