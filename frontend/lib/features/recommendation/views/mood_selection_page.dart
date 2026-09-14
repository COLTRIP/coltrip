import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../controllers/recommendation_selection_controller.dart';
import '../data/place_mood_data.dart';
import '../widgets/place_mood_selector.dart';
import '../widgets/recommendation_selection_app_bar.dart';


class MoodSelectionPage extends StatefulWidget {
  const MoodSelectionPage({super.key});

  @override
  State<MoodSelectionPage> createState() => _EmotionSelectionPageState();
}

class _EmotionSelectionPageState extends State<MoodSelectionPage> {
  void _moveToRecommendations(RecommendationSelectionController controller) {
    final requestData = controller.createRequestData();

    if (requestData == null) {
      Get.back();
      return;
    }

    Get.toNamed(AppRoutes.recommendationList, arguments: requestData);
  }

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<RecommendationSelectionController>();

    return Scaffold(
      appBar: RecommendationAppBar(
        action: RecommendationAppBarAction.next,
        onPressed: () {
          _moveToRecommendations(controller);
        },
      ),
      body: Center(
        child: Column(
          children: [
            const SizedBox(height: 150),

            const Text(
              '원하시는 여행 감성을',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w600,
                fontSize: 20,
              ),
            ),
            const Text(
              '선택해주세요 💫',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w600,
                fontSize: 20,
              ),
            ),
            const Text(
              '*필수는 아니지만, 사용자님에게 맞는 관광지를',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w300,
                fontSize: 12,
              ),
            ),
            const Text(
              '추천드리기 위해 선택해주세요!',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w300,
                fontSize: 12,
              ),
            ),

            const SizedBox(height: 30),

            Obx(
              () => Padding(
                padding: const EdgeInsets.all(30),
                child: PlaceMoodSelector(
                  items: PlaceMoodData.items,
                  selectedMoodIds: controller.selectedMoodIds.toSet(),
                  onChanged: controller.toggleMood,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
