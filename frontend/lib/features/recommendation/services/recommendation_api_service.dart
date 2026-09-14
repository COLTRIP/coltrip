import 'dart:developer' as developer;

import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../models/recommendation.dart';
import '../models/quiet_score_point.dart';

// TODO(예외처리 통합): try/catch(DioException) → ApiException 변환 반복.
//   api_exception.dart 계획대로 에러 인터셉터로 중앙화 예정.
class RecommendationApiService {
  RecommendationApiService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  // TODO: 리스트 불러올 때 일단 임의로 부산 전체
  static const _busanSwLat = 34.98;
  static const _busanSwLng = 128.75;
  static const _busanNeLat = 35.40;
  static const _busanNeLng = 129.30;

  /// GET /api/spots — bounding box 안의 추천 장소 목록
  Future<List<Spot>> getSpots({String? category, String? mode}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots',
        queryParameters: {
          'swLat': _busanSwLat,
          'swLng': _busanSwLng,
          'neLat': _busanNeLat,
          'neLng': _busanNeLng,
          if (category != null && category.isNotEmpty && category != '전체')
            'category': category,
          if (mode != null && mode.isNotEmpty) 'mode': mode,
        },
      );

      final list = response.data?['spots'] as List? ?? const [];
      return list.cast<Map<String, dynamic>>().map(Spot.fromJson).toList();
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  /// GET /api/spots/recommendations — 날짜/시간대별 예측 추천
  Future<RecommendationResult> getRecommendations({
    required DateTime dateTime,
    String? category,
    List<String> modes = const [],
  }) async {
    try {
      final requestModes = modes.isEmpty ? <String?>[null] : modes;
      final date = _formatDate(dateTime);

      developer.log(
        '추천 요청: date=$date, hour=${dateTime.hour}, '
        'category=$category, modes=$requestModes',
        name: 'RecommendationApiService',
      );

      final responses = await Future.wait(
        requestModes.map(
          (mode) => _dio.get<Map<String, dynamic>>(
            '/api/spots/recommendations',
            queryParameters: {
              'date': date,
              'hour': dateTime.hour,
              if (category != null && category.isNotEmpty) 'category': category,
              if (mode != null && mode.isNotEmpty) 'mode': mode,
              'limit': 20,
            },
            options: Options(receiveTimeout: const Duration(seconds: 60)),
          ),
        ),
      );

      final spotsById = <int, Spot>{};
      String? responseMessage;

      for (final response in responses) {
        final items = response.data?['spots'] as List? ?? const [];
        responseMessage ??= response.data?['message'] as String?;

        for (final item in items.cast<Map<String, dynamic>>()) {
          final spot = Spot.fromRecommendationJson(item);
          final previous = spotsById[spot.id];

          if (previous == null ||
              (spot.quietScore ?? 0) > (previous.quietScore ?? 0)) {
            spotsById[spot.id] = spot;
          }
        }
      }

      final spots = spotsById.values.toList()
        ..sort((a, b) => (b.quietScore ?? 0).compareTo(a.quietScore ?? 0));

      developer.log(
        '추천 응답: ${spots.length}개, message=$responseMessage',
        name: 'RecommendationApiService',
      );

      return RecommendationResult(spots: spots, message: responseMessage);
    } on DioException catch (e, stackTrace) {
      developer.log(
        '추천 API 실패'
        '\ntype=${e.type}'
        '\nmethod=${e.requestOptions.method}'
        '\nuri=${e.requestOptions.uri}'
        '\nstatusCode=${e.response?.statusCode}'
        '\nresponse=${e.response?.data}'
        '\nmessage=${e.message}',
        name: 'RecommendationApiService',
        error: e,
        stackTrace: stackTrace,
      );
      throw ApiException.fromDioException(e);
    }
  }

  String _formatDate(DateTime dateTime) {
    final month = dateTime.month.toString().padLeft(2, '0');
    final day = dateTime.day.toString().padLeft(2, '0');
    return '${dateTime.year}-$month-$day';
  }

  Future<SpotDetail> getSpotDetail({required int spotId}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId',
      );
      return SpotDetail.fromJson(response.data!);
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<bool> likeSpot({required int spotId}) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/spots/$spotId/like',
      );
      return response.data!['liked'] as bool;
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<bool> unlikeSpot({required int spotId}) async {
    try {
      final response = await _dio.delete<Map<String, dynamic>>(
        '/api/spots/$spotId/like',
      );
      return response.data!['liked'] as bool;
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<List<QuietScorePoint>> getTimeline({
    required int spotId,
    required DateTime dateTime,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId/quiet-index/forecast',
        queryParameters: {'date': _formatDate(dateTime), 'hour': dateTime.hour},
      );

      final timeline = response.data?['timeline'] as List? ?? const [];

      return timeline
          .cast<Map<String, dynamic>>()
          .where(
            (item) => item['targetAt'] != null && item['quietIndex'] != null,
          )
          .map(QuietScorePoint.fromTimelineJson)
          .toList()
        ..sort((a, b) => a.hour.compareTo(b.hour));
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }
}
