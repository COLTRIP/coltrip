import 'package:flutter/foundation.dart';

import '../../../core/network/api_exception.dart';
import '../exceptions/recommendation_exception.dart';
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
    } on ApiException catch (e) {
      errorMessage = e.message;
    } on RecommendationLoadException catch (e) {
      errorMessage = e.message;
    } catch (_) {
      errorMessage = const RecommendationLoadException().message;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}
