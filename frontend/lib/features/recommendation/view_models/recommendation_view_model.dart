import 'package:flutter/foundation.dart';

import '../models/recommendation.dart';
import '../repositories/recommendation_repository.dart';

class RecommendationViewModel extends ChangeNotifier {
  RecommendationViewModel({RecommendationRepository? repository})
    : _repository = repository ?? RecommendationRepository();

  final RecommendationRepository _repository;

  List<Spot> spots = [];
  bool isLoading = false;
  String? errorMessage;

  Future<void> loadSpots({String? category, String? mode}) async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();

    try {
      spots = await _repository.getSpots(category: category, mode: mode);
    } catch (_) {
      // 목록은 code별 특수 처리가 없어 서버/네트워크 에러 모두 동일 문구
      errorMessage = '추천 목록을 불러오지 못했어요. 다시 시도해주세요.';
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}
