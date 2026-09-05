import 'package:dio/dio.dart';

class ApiException implements Exception {
  final String message;
  final int? statusCode;

  const ApiException(this.message, {this.statusCode});

  factory ApiException.fromDioException(DioException e) {
    final statusCode = e.response?.statusCode;
    final serverMessage = e.response?.data is Map ? e.response?.data['message'] as String? : null;

    return ApiException(
      serverMessage ?? e.message ?? '알 수 없는 오류가 발생했습니다.',
      statusCode: statusCode,
    );
  }

  @override
  String toString() => message;
}
