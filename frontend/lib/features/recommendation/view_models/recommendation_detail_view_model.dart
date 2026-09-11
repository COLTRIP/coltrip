import 'package:flutter/foundation.dart';

import '../../../core/network/api_exception.dart';
import '../models/recommendation.dart';
import '../repositories/recommendation_repository.dart';

class RecommendationDetailViewModel extends ChangeNotifier {
  final int spotId;
  final RecommendationRepository _repository;

  SpotDetail? spot;
  bool isLoading = false;
  String? errorMessage;

  bool _disposed = false;

  RecommendationDetailViewModel({
    required this.spotId,
    RecommendationRepository? repository,
  }) : _repository = repository ?? RecommendationRepository() {
    loadDetail();
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

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
