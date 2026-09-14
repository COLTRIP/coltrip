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

    return RecommendationPageArguments(
      category: _toApiCategory(placeTypeId),
      categoryLabel: PlaceTypeData.items
          .firstWhere((item) => item.id == placeTypeId)
          .label,
      modes: selectedMoodIds.toList(growable: false),
    );
  }

  // 현재 추천 API enum과 화면의 장소 유형 id가 다른 항목을 변환한다.
  // EXPERIENCE는 대응하는 서버 category가 없어 전체 카테고리로 조회한다.
  String? _toApiCategory(String placeTypeId) {
    return switch (placeTypeId) {
      'NATURE' => 'PARK',
      'CULTURE' => 'GALLERY',
      'BOOK' => 'LIBRARY',
      'EXPERIENCE' => null,
      _ => placeTypeId,
    };
  }
}
