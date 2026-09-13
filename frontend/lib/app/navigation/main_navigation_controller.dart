import 'package:get/get.dart';

import '../../features/profile/controllers/profile_controller.dart';

class MainNavigationController extends GetxController {
  final currentIndex = 0.obs;

  void changeTab(int index) {
    currentIndex.value = index;

    // 프로필 탭 진입
    if (index == 2 && Get.isRegistered<ProfileController>()) {
      final profileController = Get.find<ProfileController>();

      profileController.loadProfile();
      profileController.loadLikedPlaces();
    }
  }
}
