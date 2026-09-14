import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../app/navigation/main_navigation_controller.dart';

/// 메인 화면의 지도, 추천, 프로필 탭을 전환하는 공통 하단 탐색 바입니다.
///
/// 현재 탭 상태와 탭 전환은 [MainNavigationController]에서 관리합니다.
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
          fontSize: 12,
          fontWeight: FontWeight.w500,
        ),
        unselectedLabelStyle: const TextStyle(
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
            icon: Icon(Icons.person_outline),
            activeIcon: Icon(
              Icons.person,
              color: Color(0xFF589C7E),
            ),
            label: '프로필',
          ),
        ],
      ),
    );
  }
}
