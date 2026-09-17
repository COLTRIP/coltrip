import 'package:dio/dio.dart';

import 'api_exception.dart';

/// Dio 요청을 실행하고 실패를 앱 공통 예외인 [ApiException]으로 변환합니다.
///
/// 엔드포인트별 복구 규칙이 필요한 경우 [recover]를 사용합니다.
Future<T> executeApiRequest<T>(
  Future<T> Function() request, {
  T Function(DioException error)? recover,
  void Function(DioException error, StackTrace stackTrace)? onError,
}) async {
  try {
    return await request();
  } on DioException catch (error, stackTrace) {
    onError?.call(error, stackTrace);

    if (recover != null) {
      return recover(error);
    }

    throw ApiException.fromDioException(error);
  }
}
