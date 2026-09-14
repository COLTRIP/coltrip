import '../models/recommendation.dart';
import '../services/recommendation_api_service.dart';
import '../models/quiet_score_point.dart';

class RecommendationRepository {
  RecommendationRepository({RecommendationApiService? api})
    : _api = api ?? RecommendationApiService();

  final RecommendationApiService _api;

  // TODO: 캐시(RecommendationCacheService) 도입 시 여기서 조합
  Future<List<Spot>> getSpots({String? category, String? mode}) {
    return _api.getSpots(category: category, mode: mode);
  }

  Future<RecommendationResult> getRecommendations({
    required DateTime dateTime,
    String? category,
    List<String> modes = const [],
  }) {
    return _api.getRecommendations(
      dateTime: dateTime,
      category: category,
      modes: modes,
    );
  }

  Future<SpotDetail> getSpotDetail({required int spotId}) {
    return _api.getSpotDetail(spotId: spotId);
  }

  Future<bool> likeSpot({required int spotId}) {
    return _api.likeSpot(spotId: spotId);
  }

  Future<bool> unlikeSpot({required int spotId}) {
    return _api.unlikeSpot(spotId: spotId);
  }

  Future<List<QuietScorePoint>> getTimeline({
    required int spotId,
    required DateTime dateTime,
  }) {
    return _api.getTimeline(spotId: spotId, dateTime: dateTime);
  }
}
