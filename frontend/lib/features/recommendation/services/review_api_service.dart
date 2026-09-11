import 'package:dio/dio.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../models/review.dart';

// TODO(예외처리 통합): try/catch(DioException) → ApiException 변환 반복.
//   api_exception.dart 계획대로 에러 인터셉터로 중앙화 예정 (특히 404/409 code 분기).
class ReviewApiService {

  ReviewApiService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;


  Future<List<SpotReview>> getReviews({required int spotId}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId/reviews'
      );

      final list = response.data?['reviews'] as List? ?? const [];
      return list
          .cast<Map<String, dynamic>>()
          .map(SpotReview.fromJson)
          .toList();
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  /// POST /api/visits/{visitId}/review — 방문 완료한 사용자의 리뷰 작성
  Future<SpotReview> createReview({
    required int visitId,
    required int rating,
    String? content,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/visits/$visitId/review',
        data: {
          'rating': rating,
          if (content != null && content.isNotEmpty) 'content': content,
        }
      );
      return SpotReview.fromJson(response.data!);
    } on DioException catch (e) {
      // 404 VisitNotFoundException(본인 방문 아님 포함),
      // 409 ReviewNotAllowedException(방문 미완료 / 이미 작성)
      throw ApiException.fromDioException(e);
    }
  }
}
