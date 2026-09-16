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
  String? emptyMessage;
  int _requestId = 0;
  bool _disposed = false;

  void _safeNotify() {
    if (!_disposed) notifyListeners();
  }

  Future<void> loadSpots({String? category, String? mode}) async {
    isLoading = true;
    errorMessage = null;
    emptyMessage = null;
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

  Future<void> loadRecommendations({
    required DateTime dateTime,
    String? category,
    List<String> modes = const [],
  }) async {
    final requestId = ++_requestId;
    isLoading = true;
    errorMessage = null;
    emptyMessage = null;
    _safeNotify();

    try {
      final result = await _repository.getRecommendations(
        dateTime: dateTime,
        category: category,
        modes: modes,
      );
      if (requestId == _requestId) {
        spots = result.spots;
        emptyMessage = result.message;
      }
    } catch (error) {
      debugPrint('추천 목록 조회 실패: $error');
      if (requestId == _requestId) {
        errorMessage = '추천 목록을 불러오지 못했어요. 다시 시도해주세요.';
      }
    } finally {
      if (requestId == _requestId) {
        isLoading = false;
        _safeNotify();
      }
    }
  }

  Future<void> loadCurrentRecommendations({
    String? category,
    List<String> modes = const [],
  }) async {
    final requestId = ++_requestId;
    isLoading = true;
    errorMessage = null;
    emptyMessage = null;
    _safeNotify();

    try {
      final result = await _repository.getCurrentRecommendations(
        category: category,
        modes: modes,
      );
      if (requestId == _requestId) {
        spots = result.spots;
        emptyMessage = result.message;
      }
    } catch (error) {
      debugPrint('현재 추천 목록 조회 실패: $error');
      if (requestId == _requestId) {
        errorMessage = '추천 목록을 불러오지 못했어요. 다시 시도해주세요.';
      }
    } finally {
      if (requestId == _requestId) {
        isLoading = false;
        _safeNotify();
      }
    }
  }

  @override
  void dispose() {
    _disposed = true;
    _requestId++;
    super.dispose();
  }
}
