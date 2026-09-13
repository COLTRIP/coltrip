import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../models/place_review.dart';

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
    } on DioException {
      throw const ReviewException('리뷰를 불러오지 못했습니다.');
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
        throw const ReviewException('수정된 리뷰 정보를 받지 못했습니다.');
      }

      return PlaceReview.fromJson(data);
    } on DioException catch (error) {
      final statusCode = error.response?.statusCode;

      if (statusCode == 400) {
        throw const ReviewException('별점은 1점부터 5점까지 선택해주세요.');
      }

      if (statusCode == 401) {
        throw const ReviewException('로그인이 필요합니다.');
      }

      if (statusCode == 404) {
        throw const ReviewException('리뷰를 찾을 수 없거나 수정 권한이 없습니다.');
      }

      throw const ReviewException('리뷰를 수정하지 못했습니다.');
    } on ReviewException {
      rethrow;
    }
  }

  Future<void> deleteReview({required int reviewId}) async {
    try {
      await _dio.delete<void>('/api/reviews/$reviewId');
    } on DioException catch (error) {
      final statusCode = error.response?.statusCode;

      if (statusCode == 401) {
        throw const ReviewException('로그인이 필요합니다.');
      }

      if (statusCode == 404) {
        throw const ReviewException('리뷰를 찾을 수 없거나 삭제 권한이 없습니다.');
      }

      throw const ReviewException('리뷰를 삭제하지 못했습니다.');
    }
  }
}

class ReviewException implements Exception {
  const ReviewException(this.message);

  final String message;

  @override
  String toString() => message;
}
