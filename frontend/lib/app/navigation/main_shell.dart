import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../features/map/views/map_page.dart';
import '../../features/profile/views/profile_page.dart';
import '../../features/recommendation/views/place_selection_page.dart';
import '../../shared/widgets/app_bottom_navigation_bar.dart';
import 'main_navigation_controller.dart';

/// 지도, 추천, 프로필 탭을 유지하면서 전환하는 메인 화면입니다.
///
/// [IndexedStack]을 사용해 탭을 변경해도 각 화면의 상태가 유지됩니다.
class MainShell extends StatelessWidget {
  const MainShell({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<MainNavigationController>();

    return Obx(() {
      final currentIndex = controller.currentIndex.value;

      return Scaffold(
        body: IndexedStack(
          index: currentIndex,
          children: [
            MapPage(isActive: currentIndex == 0),
            const PlaceSelectionPage(),
            const ProfilePage(),
          ],
        ),
        bottomNavigationBar: const AppBottomNavigationBar(),
      );
    });
  }
}
