import 'package:dio/dio.dart';

/// API 요청 실패 정보를 화면 계층에 전달하기 위한 공통 예외입니다.
///
/// Dio의 네트워크 오류와 서버 오류 응답을 앱에서 사용하기 쉬운 형태로
/// 변환하며, 사용자 메시지와 HTTP 상태 코드 및 서버 오류 코드를 보관합니다.
class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final String? code;

  const ApiException(this.message, {this.statusCode, this.code});

  /// Dio 오류를 앱에서 공통으로 처리하는 예외 형태로 변환합니다.
  factory ApiException.fromDioException(DioException e) {
    if (e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.sendTimeout ||
        e.type == DioExceptionType.connectionError) {
      return const ApiException('네트워크 연결을 확인해주세요.', code: 'NETWORK');
    }

    final data = e.response?.data;
    final serverCode = data is Map ? data['code']?.toString() : null;
    final serverMessage = data is Map ? data['message']?.toString() : null;

    return ApiException(
      serverMessage ?? e.message ?? '알 수 없는 오류가 발생했습니다.',
      statusCode: e.response?.statusCode,
      code: serverCode,
    );
  }

  @override
  String toString() => message;
}
