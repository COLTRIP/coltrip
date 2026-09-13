import 'package:coltrip/features/recommendation/services/review_api_service.dart';

import '../models/review.dart';

class ReviewRepository {
  final ReviewApiService _api;
  
  ReviewRepository({ReviewApiService? api}) : _api = api ?? ReviewApiService();


  Future<List<SpotReview>> getReviews({required int spotId}) {
    return _api.getReviews(spotId: spotId);
  }

  Future<SpotReview> createReview({
    required int visitId,
    required int rating,
    String? content,
  }) {
    return _api.createReview(
      visitId: visitId,
      rating: rating,
      content: content,
    );
  }
}