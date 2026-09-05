import 'package:flutter/foundation.dart';

import '../exceptions/recommendation_exception.dart';
import '../models/recommendation.dart';

class RecommendationViewModel extends ChangeNotifier {
  List<Spot> spots = [];
  bool isLoading = false;
  String? errorMessage;

  RecommendationViewModel() {
    loadSpots();
  }

  Future<void> loadSpots() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();

    try {
      spots = await _fetchDummySpots(); // TODO: 실제 API 연결되면 repository 호출로 교체
    } on RecommendationLoadException catch (e) {
      errorMessage = e.message;
    } catch (e) {
      errorMessage = const RecommendationLoadException().message;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<List<Spot>> _fetchDummySpots() async {
    await Future.delayed(const Duration(milliseconds: 300));
    return [
      Spot(
        id: 1,
        name: '을숙도 생태공원',
        category: 'PARK',
        modes: const ['CONTEMPLATION', 'SCENERY'],
        latitude: 35.1039,
        longitude: 128.9553,
        quietScore: 98,
        quietScoreUpdatedAt: DateTime.now(),
        address: '부산광역시 사하구 하단동 1207',
        imageUrl: 'https://placehold.co/400x300/png?text=Spot+1',
      ),
      Spot(
        id: 2,
        name: '을숙도 생태공원',
        category: 'PARK',
        modes: const ['CONTEMPLATION', 'SCENERY'],
        latitude: 35.1039,
        longitude: 128.9553,
        quietScore: 98,
        quietScoreUpdatedAt: DateTime.now(),
        address: '부산광역시 사하구 하단동 1207',
        imageUrl: 'https://placehold.co/400x300/png?text=Spot+2',
      ),
      Spot(
        id: 3,
        name: '을숙도 생태공원',
        category: 'PARK',
        modes: const ['CONTEMPLATION', 'SCENERY'],
        latitude: 35.1039,
        longitude: 128.9553,
        quietScore: 98,
        quietScoreUpdatedAt: DateTime.now(),
        address: '부산광역시 사하구 하단동 1207',
        imageUrl: 'https://placehold.co/400x300/png?text=Spot+3',
      ),
    ];
  }
}
