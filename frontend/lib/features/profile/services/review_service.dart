import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../models/place_review.dart';

/// 장소 리뷰 관련 API 통신을 담당하는 서비스입니다.
///
/// 사용자가 작성한 리뷰를 조회하고, 리뷰 수정 및 삭제 요청을 처리합니다.
class ReviewService {
  ReviewService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  Future<PlaceReview?> getReviewBySpot({
    required int spotId,
    required int reviewId,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId/reviews',
      );

      final reviews = response.data?['reviews'];
      if (reviews is! List) return null;

      for (final item in reviews) {
        if (item is! Map<String, dynamic>) continue;

        final id = (item['id'] as num?)?.toInt();
        if (id == reviewId) {
          return PlaceReview.fromJson(item);
        }
      }

      return null;
    } on DioException catch (error) {
      throw ApiException.fromDioException(error);
    }
  }

  Future<PlaceReview> updateReview({
    required int reviewId,
    required int rating,
    String? content,
  }) async {
    try {
      final trimmedContent = content?.trim();

      final response = await _dio.patch<Map<String, dynamic>>(
        '/api/reviews/$reviewId',
        data: {
          'rating': rating,
          'content': trimmedContent == null || trimmedContent.isEmpty
              ? null
              : trimmedContent,
        },
      );

      final data = response.data;

      if (data == null) {
        throw const ApiException('수정된 리뷰 정보를 받지 못했습니다.');
      }

      return PlaceReview.fromJson(data);
    } on DioException catch (error) {
      final statusCode = error.response?.statusCode;

      if (statusCode == 400) {
        throw const ApiException('별점은 1점부터 5점까지 선택해주세요.');
      }

      if (statusCode == 401) {
        throw const ApiException('로그인이 필요합니다.', statusCode: 401);
      }

      if (statusCode == 404) {
        throw const ApiException('리뷰를 찾을 수 없거나 수정 권한이 없습니다.', statusCode: 404);
      }

      throw ApiException.fromDioException(error);
    } on ApiException {
      rethrow;
    }
  }

  Future<void> deleteReview({required int reviewId}) async {
    try {
      await _dio.delete<void>('/api/reviews/$reviewId');
    } on DioException catch (error) {
      final statusCode = error.response?.statusCode;

      if (statusCode == 401) {
        throw const ApiException('로그인이 필요합니다.', statusCode: 401);
      }

      if (statusCode == 404) {
        throw const ApiException('리뷰를 찾을 수 없거나 삭제 권한이 없습니다.', statusCode: 404);
      }

      throw ApiException.fromDioException(error);
    }
  }
}
