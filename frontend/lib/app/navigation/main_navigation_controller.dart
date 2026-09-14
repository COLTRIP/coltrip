import 'package:get/get.dart';

import '../../features/profile/controllers/profile_controller.dart';

/// 메인 화면의 현재 탭과 탭 전환 동작을 관리합니다.
class MainNavigationController extends GetxController {
  final currentIndex = 0.obs;

  void changeTab(int index) {
    currentIndex.value = index;

    // 다른 화면에서 변경됐을 수 있는 프로필 데이터를 탭 진입 시 갱신합니다.
    if (index == 2 && Get.isRegistered<ProfileController>()) {
      final profileController = Get.find<ProfileController>();

      profileController.loadAll();
    }
  }
}
