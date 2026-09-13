import 'package:get/get.dart';

import '../../features/profile/controllers/profile_controller.dart';
import '../../features/recommendation/controllers/recommendation_selection_controller.dart';

class MainShellBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<ProfileController>(() => ProfileController(), fenix: true);

    Get.lazyPut<RecommendationSelectionController>(
      () => RecommendationSelectionController(),
      fenix: true,
    );
  }
}
