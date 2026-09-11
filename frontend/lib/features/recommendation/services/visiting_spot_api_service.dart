import 'package:coltrip/features/recommendation/models/alternative_spot.dart';
import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../models/recommendation.dart';

// TODO(예외처리 통합): 아래 메서드마다 반복되는 try/catch(DioException) → ApiException 변환은
//   api_exception.dart 의 통합 계획대로 에러 인터셉터로 옮길 예정. 그때 여기 catch 들 제거.
class VisitingSpotApiService {
  VisitingSpotApiService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;


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
          'stayDurationSeconds': stayDurationSeconds, //없어질 예정
        },
      );
    } on DioException catch (e) {
      // 400 VisitConditionNotMetException(반경/체류시간 미충족),
      // 409 InvalidVisitStateException(이미 완료/취소) → 메시지는 ApiException 으로 전달

      throw ApiException.fromDioException(e);
    }
  }

  /// GET /api/spots/{spotId}/alternatives — 혼잡 시 유사 분위기의 더 한적한 대체지 목록.
  /// 대체지가 없으면 빈 배열(200).
  Future<List<AlternativeSpot>> getAlternatives({
    required int spotId,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId/alternatives',
      );

      final list = response.data?['alternatives'] as List? ?? const [];
      return list
          .cast<Map<String, dynamic>>()
          .map(AlternativeSpot.fromJson)
          .toList();
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }



  /// 현재 고요지수 재조회. 아직 계산 안 된 스팟이면 null.
  Future<int?> viewQuietValue({
    required int spotId,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId',
      );
      return SpotDetail.fromJson(response.data!).quietScore;
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

}
