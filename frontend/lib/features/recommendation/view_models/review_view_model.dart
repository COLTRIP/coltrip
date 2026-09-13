import 'package:coltrip/features/recommendation/repositories/review_repository.dart';
import 'package:flutter/foundation.dart';

import '../../../core/network/api_exception.dart';
import '../models/review.dart';

class ReviewViewModel extends ChangeNotifier {
  ReviewViewModel({required this.spotId, ReviewRepository? repository}) : _repository = repository ?? ReviewRepository() {
    loadReviews();
  }

  final int spotId;
  final ReviewRepository _repository;

  List<SpotReview> reviews = [];
  bool isLoading = false;
  String? errorMessage;

  Future<void> loadReviews() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();

    try {
      reviews = await _repository.getReviews(spotId: spotId);
    } on ApiException catch (e) {
      errorMessage = e.message;
    } catch (_) {
      errorMessage = '리뷰를 불러오지 못했어요. 다시 시도해주세요.';
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}
