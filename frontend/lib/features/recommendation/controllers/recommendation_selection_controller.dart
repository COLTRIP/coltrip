import 'package:get/get.dart';

import '../data/place_type_data.dart';
import '../models/place_mood_item.dart';
import '../models/place_type_item.dart';
import '../models/recommendation_page_arguments.dart';

class RecommendationSelectionController extends GetxController {
  final selectedPlaceTypeId = RxnString();

  // 무드는 선택 사항이므로 기본값 없이 시작
  final selectedMoodIds = <String>[].obs;

  void selectPlaceType(PlaceTypeItem item) {
    selectedPlaceTypeId.value = item.id;
  }

  void toggleMood(PlaceMoodItem mood) {
    if (selectedMoodIds.contains(mood.id)) {
      selectedMoodIds.remove(mood.id);
    } else {
      selectedMoodIds.add(mood.id);
    }
  }

  RecommendationPageArguments? createRequestData() {
    final placeTypeId = selectedPlaceTypeId.value;

    // 장소 유형은 필수
    if (placeTypeId == null) {
      return null;
    }

    final selectedPlaceType = PlaceTypeData.items.firstWhere(
      (item) => item.id == placeTypeId,
    );

    return RecommendationPageArguments(
      category: selectedPlaceType.apiCategory,
      categoryLabel: selectedPlaceType.label,
      modes: selectedMoodIds.toList(growable: false),
    );
  }
}
