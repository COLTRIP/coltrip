import 'package:flutter/foundation.dart';

import '../exceptions/recommendation_exception.dart';
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
      spot = await _repository.getSpotDetail(
        spotId: spotId,
      ); // TODO: 실제 API 연결되면 repository 호출로 교체
    } on RecommendationLoadException catch (e) {
      errorMessage = e.message;
    } catch (e) {
      errorMessage = const RecommendationLoadException(
        '상세 정보를 불러오지 못했어요. 다시 시도해주세요.',
      ).message;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}
