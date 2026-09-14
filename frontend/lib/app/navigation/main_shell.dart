import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../features/map/views/map_page.dart';
import '../../features/profile/views/profile_page.dart';
import '../../features/recommendation/views/place_selection_page.dart';
import '../../shared/widgets/app_bottom_navigation_bar.dart';
import 'main_navigation_controller.dart';

class MainShell extends StatelessWidget {
  MainShell({super.key});

  final MainNavigationController controller = Get.put(
    MainNavigationController(),
  );

  @override
  Widget build(BuildContext context) {
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
