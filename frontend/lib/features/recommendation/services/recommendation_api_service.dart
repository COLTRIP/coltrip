import 'dart:developer' as developer;

import 'package:dio/dio.dart';

import '../../../core/network/api_request.dart';
import '../../../core/network/dio_client.dart';
import '../models/recommendation.dart';
import '../models/quiet_score_point.dart';

class RecommendationApiService {
  RecommendationApiService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  /// GET /api/spots/recommendations — 날짜/시간대별 예측 추천
  Future<RecommendationResult> getRecommendations({
    required DateTime dateTime,
    String? category,
    List<String> modes = const [],
  }) {
    return executeApiRequest(() async {
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

      final spots = spotsById.values.toList()..sort(_compareQuietScore);

      developer.log(
        '추천 응답: ${spots.length}개, message=$responseMessage',
        name: 'RecommendationApiService',
      );

      return RecommendationResult(spots: spots, message: responseMessage);
    }, onError: _logRecommendationError);
  }

  /// GET /api/spots/recommendations/current — 현재 고요지수 기준 추천
  Future<RecommendationResult> getCurrentRecommendations({
    String? category,
    List<String> modes = const [],
  }) {
    return executeApiRequest(() async {
      final requestModes = modes.isEmpty ? <String?>[null] : modes;
      final responses = await Future.wait(
        requestModes.map(
          (mode) => _dio.get<Map<String, dynamic>>(
            '/api/spots/recommendations/current',
            queryParameters: {
              if (category != null && category.isNotEmpty) 'category': category,
              if (mode != null && mode.isNotEmpty) 'mode': mode,
              'limit': 20,
            },
          ),
        ),
      );

      final spotsById = <int, Spot>{};
      String? responseMessage;
      for (final response in responses) {
        final items = response.data?['spots'] as List? ?? const [];
        responseMessage ??= response.data?['message'] as String?;
        for (final item in items.cast<Map<String, dynamic>>()) {
          final spot = Spot.fromCurrentRecommendationJson(item);
          final previous = spotsById[spot.id];
          if (previous == null ||
              (spot.quietScore ?? -1) > (previous.quietScore ?? -1)) {
            spotsById[spot.id] = spot;
          }
        }
      }

      final spots = spotsById.values.toList()..sort(_compareQuietScore);
      return RecommendationResult(spots: spots, message: responseMessage);
    });
  }

  int _compareQuietScore(Spot a, Spot b) {
    final aScore = a.quietScore;
    final bScore = b.quietScore;
    if (aScore == null && bScore == null) return a.id.compareTo(b.id);
    if (aScore == null) return 1;
    if (bScore == null) return -1;
    final scoreOrder = bScore.compareTo(aScore);
    return scoreOrder == 0 ? a.id.compareTo(b.id) : scoreOrder;
  }

  String _formatDate(DateTime dateTime) {
    final month = dateTime.month.toString().padLeft(2, '0');
    final day = dateTime.day.toString().padLeft(2, '0');
    return '${dateTime.year}-$month-$day';
  }

  Future<SpotDetail> getSpotDetail({required int spotId}) {
    return executeApiRequest(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId',
      );
      return SpotDetail.fromJson(response.data!);
    });
  }

  Future<bool> likeSpot({required int spotId}) {
    return executeApiRequest(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/spots/$spotId/like',
      );
      return response.data!['liked'] as bool;
    });
  }

  Future<bool> unlikeSpot({required int spotId}) {
    return executeApiRequest(() async {
      final response = await _dio.delete<Map<String, dynamic>>(
        '/api/spots/$spotId/like',
      );
      return response.data!['liked'] as bool;
    });
  }

  Future<List<QuietScorePoint>> getTimeline({
    required int spotId,
    required DateTime dateTime,
  }) {
    return executeApiRequest(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId/quiet-index/forecast',
        queryParameters: {'date': _formatDate(dateTime), 'hour': dateTime.hour},
      );

      final timeline = response.data?['timeline'] as List? ?? const [];

      return timeline
          .cast<Map<String, dynamic>>()
          .where((item) => item['targetAt'] != null)
          .map(QuietScorePoint.fromTimelineJson)
          .toList()
        ..sort((a, b) => a.targetAt.compareTo(b.targetAt));
    });
  }

  void _logRecommendationError(DioException error, StackTrace stackTrace) {
    developer.log(
      '추천 API 실패'
      '\ntype=${error.type}'
      '\nmethod=${error.requestOptions.method}'
      '\nuri=${error.requestOptions.uri}'
      '\nstatusCode=${error.response?.statusCode}'
      '\nresponse=${error.response?.data}'
      '\nmessage=${error.message}',
      name: 'RecommendationApiService',
      error: error,
      stackTrace: stackTrace,
    );
  }
}
