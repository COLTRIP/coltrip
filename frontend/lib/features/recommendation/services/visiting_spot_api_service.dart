import 'package:coltrip/features/recommendation/models/alternative_spot.dart';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/api_request.dart';
import '../../../core/network/dio_client.dart';
import '../models/current_visit.dart';
import '../models/recommendation.dart';

class VisitingSpotApiService {
  VisitingSpotApiService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  Future<int> startVisit({required int spotId}) {
    return executeApiRequest(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/visits/start',
        data: {'spotId': spotId},
      );
      return response.data!['visitId'] as int;
    });
  }

  Future<void> completeVisit({required int visitId}) {
    return executeApiRequest(() async {
      await _dio.patch<void>('/api/visits/$visitId/complete');
    });
  }

  /// GET /api/spots/{spotId}/alternatives — 혼잡 시 유사 분위기의 더 한적한 대체지 목록.
  /// 대체지가 없으면 빈 배열(200).
  Future<List<AlternativeSpot>> getAlternatives({required int spotId}) {
    return executeApiRequest(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId/alternatives',
      );

      final list = response.data?['alternatives'] as List? ?? const [];
      return list
          .cast<Map<String, dynamic>>()
          .map(AlternativeSpot.fromJson)
          .toList();
    });
  }

  Future<void> cancelVisit({required int visitId}) {
    return executeApiRequest(() async {
      await _dio.patch<Map<String, dynamic>>('/api/visits/$visitId/cancel');
    });
  }

  Future<CurrentVisit?> getCurrentVisit() {
    return executeApiRequest(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/visits/current',
      );
      final visit = response.data?['visit'] as Map<String, dynamic>?;
      debugPrint('[VisitingSpotApiService] 현재 방문 응답: $visit');
      return visit == null ? null : CurrentVisit.fromJson(visit);
    }, recover: _recoverMissingCurrentVisit);
  }

  /// 현재 고요지수 재조회. 아직 계산 안 된 스팟이면 null.
  Future<int?> viewQuietValue({required int spotId}) {
    return executeApiRequest(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId',
      );
      return SpotDetail.fromJson(response.data!).quietScore;
    });
  }

  CurrentVisit? _recoverMissingCurrentVisit(DioException error) {
    if (error.response?.statusCode == 404) return null;
    throw ApiException.fromDioException(error);
  }
}
