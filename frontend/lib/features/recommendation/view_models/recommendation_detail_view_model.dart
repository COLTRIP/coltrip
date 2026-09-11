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

  RecommendationDetailViewModel({
    required this.spotId,
    RecommendationRepository? repository,
  }) : _repository = repository ?? RecommendationRepository() {
    loadDetail();
  }

  Future<void> loadDetail() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();

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
      notifyListeners();
    }
  }
}
