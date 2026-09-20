import 'package:flutter/foundation.dart';

import '../../../core/network/api_exception.dart';
import '../models/recommendation.dart';
import '../repositories/recommendation_repository.dart';
import '../models/quiet_score_point.dart';

class RecommendationDetailViewModel extends ChangeNotifier {
  final int spotId;
  final RecommendationRepository _repository;
  final DateTime timelineDateTime;

  SpotDetail? spot;
  bool isLoading = false;
  String? errorMessage;

  bool _disposed = false;

  List<QuietScorePoint> timelinePoints = [];
  bool isTimelineLoading = false;

  RecommendationDetailViewModel({
    required this.spotId,
    DateTime? timelineDateTime,
    RecommendationRepository? repository,
  }) : timelineDateTime = timelineDateTime ?? DateTime.now(),
       _repository = repository ?? RecommendationRepository() {
    loadDetail();
    loadTimeline();
  }

  /// dispose 이후 비동기 콜백이 늦게 도착해도 죽지 않도록
  void _safeNotify() {
    if (_disposed) return;
    notifyListeners();
  }

  Future<void> loadDetail() async {
    isLoading = true;
    errorMessage = null;
    _safeNotify();

    try {
      spot = await _repository.getSpotDetail(spotId: spotId);
    } on ApiException catch (e) {
      errorMessage = switch (e.code) {
        'SpotNotFoundException' => '삭제되었거나 존재하지 않는 장소예요.', // 404
        _ => e.message,
      };
    } catch (_) {
      errorMessage = '상세 정보를 불러오지 못했어요. 다시 시도해주세요.';
    } finally {
      isLoading = false;
      _safeNotify();
    }
  }

  Future<void> toggleLike() async {
    if (spot == null) return;

    final wasLiked = spot!.isLiked;
    spot = spot!.copyWith(isLiked: !wasLiked);
    _safeNotify();

    var liked = !wasLiked;
    try {
      liked = wasLiked
          ? await _repository.unlikeSpot(spotId: spotId)
          : await _repository.likeSpot(spotId: spotId);
    } catch (_) {
      liked = wasLiked; // 실패 시 낙관적 업데이트 롤백
    }

    if (_disposed || spot == null) return;
    spot = spot!.copyWith(isLiked: liked);
    _safeNotify();
  }

  Future<void> loadTimeline() async {
    isTimelineLoading = true;
    _safeNotify();

    try {
      final now = DateTime.now();
      final baseHour = DateTime(
        now.year,
        now.month,
        now.day,
        timelineDateTime.hour,
      );

      // API는 24시간 단위 응답을 유지하므로, 오늘부터 7일의
      // 같은 시간대를 각각 조회해 주간 데이터를 구성한다.
      timelinePoints = await Future.wait(
        List.generate(7, (index) async {
          final target = baseHour.add(Duration(days: index));

          try {
            final points = await _repository.getTimeline(
              spotId: spotId,
              dateTime: target,
            );

            for (final point in points) {
              if (_isSameHour(point.targetAt, target)) return point;
            }
          } catch (_) {
            // 한 요일 조회 실패가 일주일 전체 그래프를 가리지 않도록 빈 값 처리
          }

          return QuietScorePoint(targetAt: target, score: null);
        }),
      );
    } catch (_) {
      timelinePoints = [];
    } finally {
      isTimelineLoading = false;
      _safeNotify();
    }
  }

  bool _isSameHour(DateTime a, DateTime b) {
    return a.year == b.year &&
        a.month == b.month &&
        a.day == b.day &&
        a.hour == b.hour;
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
