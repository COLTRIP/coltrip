import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../controllers/recommendation_selection_controller.dart';
import '../widgets/recommendation_selection_app_bar.dart';
import '../../../app/routes/app_routes.dart';
import '../data/place_type_data.dart';
import '../widgets/place_type_grid.dart';

class PlaceSelectionPage extends StatelessWidget {
  const PlaceSelectionPage({super.key});

  void _moveToMoodSelection(RecommendationSelectionController controller) {
    final selectedPlaceTypeId = controller.selectedPlaceTypeId.value;

    if (selectedPlaceTypeId == null) {
      // 버튼을 여러 번 눌렀을 때 스낵바가 겹치는 것 방지
      if (Get.isSnackbarOpen) {
        Get.closeCurrentSnackbar();
      }

      Get.snackbar(
        '장소 유형을 선택해주세요!',
        '추천받고 싶은 장소 유형을 하나 선택해야 해요.',
        snackPosition: SnackPosition.BOTTOM,
        margin: const EdgeInsets.all(16),
        borderRadius: 12,
        backgroundColor: const Color(0xFF589C7E),
        colorText: Colors.white,
        duration: const Duration(seconds: 2),
      );

      return;
    }

    Get.toNamed(AppRoutes.moodSelection);
  }

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<RecommendationSelectionController>();

    return Scaffold(
      appBar: RecommendationAppBar(
        showBackButton: false,
        onPressed: () {
          _moveToMoodSelection(controller);
        },
      ),
      body: Column(
        children: [
          const SizedBox(height: 20),

          const Text(
            '원하시는 장소 유형을',
            style: TextStyle(
              fontWeight: FontWeight.w600,
              fontSize: 20,
            ),
          ),
          const Text(
            '선택해주세요📍',
            style: TextStyle(
              fontWeight: FontWeight.w600,
              fontSize: 20,
            ),
          ),

          const SizedBox(height: 20),

          Expanded(
            child: Obx(
              () => PlaceTypeGrid(
                items: PlaceTypeData.items,
                selectedId: controller.selectedPlaceTypeId.value,
                onSelected: controller.selectPlaceType,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
