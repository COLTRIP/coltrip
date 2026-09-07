import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../app/navigation/main_navigation_controller.dart';


class AppBottomNavigationBar extends GetView<MainNavigationController> {
  const AppBottomNavigationBar({super.key});

  @override
  Widget build(BuildContext context) {
    return Obx(
          () => BottomNavigationBar(
        currentIndex: controller.currentIndex.value,
        onTap: controller.changeTab,

        backgroundColor: const Color(0xFFF7F9F8),
        selectedItemColor: const Color(0xFF252B28),
        unselectedItemColor: const Color(0xFF252B28),

        selectedLabelStyle: const TextStyle(
          fontFamily: 'Paperlogy',
          fontSize: 12,
          fontWeight: FontWeight.w500,
        ),
        unselectedLabelStyle: const TextStyle(
          fontFamily: 'Paperlogy',
          fontSize: 12,
          fontWeight: FontWeight.w300,
        ),

        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.map_outlined),
            activeIcon: Icon(
              Icons.map_sharp,
              color: Color(0xFF589C7E),
            ),
            label: '지도',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.auto_awesome_outlined),
            activeIcon: Icon(
              Icons.auto_awesome,
              color: Color(0xFF589C7E),
            ),
            label: '추천',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings_outlined),
            activeIcon: Icon(
              Icons.settings,
              color: Color(0xFF589C7E),
            ),
            label: '내정보',
          ),
        ],
      ),
    );
  }
}