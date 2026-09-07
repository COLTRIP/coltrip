import 'package:dio/dio.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../models/review.dart';

class ReviewApiService {

  ReviewApiService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  // TODO: 로그인 연동 전 임시. 협업자에게 받은 테스트용 accessToken을 넣을 것.
  static const _tempAccessToken = '';

  Future<List<SpotReview>> getReviews({required int spotId}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId/reviews',
        options: Options(
          headers: {'Authorization': 'Bearer $_tempAccessToken'},
        ),
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
        },
        options: Options(
          headers: {'Authorization': 'Bearer $_tempAccessToken'},
        ),
      );
      return SpotReview.fromJson(response.data!);
    } on DioException catch (e) {
      // 404 VisitNotFoundException(본인 방문 아님 포함),
      // 409 ReviewNotAllowedException(방문 미완료 / 이미 작성)
      throw ApiException.fromDioException(e);
    }
  }
}
