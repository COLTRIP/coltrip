import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';

class RecommendationService {
  RecommendationService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  Future<void> requestRecommendation({
    required String placeTypeId,
    required String mood,
  }) async {
    await _dio.post<void>(
      '실제 추천 API 경로',
      data: {'placeTypeId': placeTypeId, 'mood': mood},
    );
  }
}
