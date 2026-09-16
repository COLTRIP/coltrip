import 'package:dio/dio.dart';

import '../../../core/network/api_request.dart';
import '../../../core/network/dio_client.dart';
import '../models/review.dart';

class ReviewApiService {
  ReviewApiService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  Future<List<SpotReview>> getReviews({required int spotId}) {
    return executeApiRequest(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId/reviews',
      );

      final list = response.data?['reviews'] as List? ?? const [];
      return list
          .cast<Map<String, dynamic>>()
          .map(SpotReview.fromJson)
          .toList();
    });
  }

  /// POST /api/visits/{visitId}/review — 방문 완료한 사용자의 리뷰 작성
  Future<void> createReview({
    required int visitId,
    required int rating,
    String? content,
  }) {
    return executeApiRequest(() async {
      await _dio.post<void>(
        '/api/visits/$visitId/review',
        data: {
          'rating': rating,
          if (content != null && content.isNotEmpty) 'content': content,
        },
      );
    });
  }
}
