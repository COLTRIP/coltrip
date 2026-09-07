import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';

class VisitingSpotApiService {
  VisitingSpotApiService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  // TODO: 로그인 연동 전 임시. 협업자에게 받은 테스트용 accessToken을 넣을 것.
  static const _tempAccessToken = '';

  Future<int> startVisit({
    required int spotId,
    required double startLatitude,
    required double startLongitude,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/visits/start',
        data: {
          'spotId': spotId,
          'startLatitude': startLatitude,
          'startLongitude': startLongitude,
        },
        options: Options(
          headers: {'Authorization': 'Bearer $_tempAccessToken'},
        ),
      );
      return response.data!['visitId'] as int;
    } on DioException catch (e) {
      // 409 AlreadyOngoingVisitException → 진행 중 방문 복구 로직에서 처리
      throw ApiException.fromDioException(e);
    }
  }

  Future<void> completeVisit({
    required int visitId,
    required double arrivedLatitude,
    required double arrivedLongitude,
    required int stayDurationSeconds,
  }) async {
    try {
      await _dio.patch<Map<String, dynamic>>(
        '/api/visits/$visitId/complete',
        data: {
          'arrivedLatitude': arrivedLatitude,
          'arrivedLongitude': arrivedLongitude,
          'stayDurationSeconds': stayDurationSeconds,
        },
        options: Options(
          headers: {'Authorization': 'Bearer $_tempAccessToken'},
        ),
      );
    } on DioException catch (e) {
      // 400 VisitConditionNotMetException(반경/체류시간 미충족),
      // 409 InvalidVisitStateException(이미 완료/취소) → 메시지는 ApiException 으로 전달
      throw ApiException.fromDioException(e);
    }
  }
}
