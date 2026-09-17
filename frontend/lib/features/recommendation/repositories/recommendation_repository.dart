import '../models/recommendation.dart';
import '../models/quiet_score_point.dart';
import '../services/recommendation_api_service.dart';

class RecommendationRepository {
  RecommendationRepository({RecommendationApiService? api})
    : _api = api ?? RecommendationApiService();

  final RecommendationApiService _api;

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

  Future<RecommendationResult> getCurrentRecommendations({
    String? category,
    List<String> modes = const [],
  }) {
    return _api.getCurrentRecommendations(category: category, modes: modes);
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
