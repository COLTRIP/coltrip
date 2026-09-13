import 'package:coltrip/features/profile/views/liked_places_page.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../data/dummy_places.dart';
import '../controllers/profile_controller.dart';
import '../views/account_management_page.dart';
import '../views/visited_places_page.dart';
import '../widgets/place_carousel_section.dart';
import '../widgets/setting_menu_button.dart';
import '../widgets/user_info_card.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  late final ProfileController controller;

  @override
  void initState() {
    super.initState();

    if (Get.isRegistered<ProfileController>()) {
      controller = Get.find<ProfileController>();
    } else {
      controller = Get.put(ProfileController());
    }

    controller.loadProfile();
    controller.loadLikedPlaces();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: ListView(
        padding: EdgeInsets.zero,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 20),
            child: Obx(() {
              if (controller.isLoading.value &&
                  controller.nickname.value.isEmpty) {
                return const SizedBox(
                  height: 130,
                  child: Center(child: CircularProgressIndicator()),
                );
              }

              return UserInfoCard(
                nickname: controller.nickname.value.isEmpty
                    ? '사용자 님'
                    : '${controller.nickname.value} 님',
                visitedPlaceCount: controller.visitedPlaceCount.value,
                likedPlaceCount: controller.likedPlaceCount.value,
              );
            }),
          ),

          PlaceCarouselSection(
            title: '방문한 장소',
            places: visitedPlaces,
            onMorePressed: () {
              Get.to(() => const VisitedPlacesPage());
            },
            emptyMessage: '아직 방문한 장소가 없어요.',
          ),

          const SizedBox(height: 10),

          Obx(() {
            if (controller.isLikedPlacesLoading.value &&
                controller.likedPlaces.isEmpty) {
              return const SizedBox(
                height: 200,
                child: Center(child: CircularProgressIndicator()),
              );
            }

            return PlaceCarouselSection(
              title: '좋아요한 장소',
              places: controller.likedPlaces.toList(),
              onMorePressed: () {
                Get.to(() => const LikedPlacesPage());
              },
              emptyMessage: '아직 좋아요한 장소가 없어요.',
            );
          }),

          const Divider(
            thickness: 0.5,
            color: Color(0x33252B28),
            indent: 20,
            endIndent: 20,
            height: 40,
          ),

          // SettingMenuButton(
          //   title: '알림 설정',
          //   onTap: () {
          //     // TODO: 알림 설정 페이지 이동
          //   },
          // ),
          SettingMenuButton(
            title: '계정 관리',
            onTap: () {
              Get.to(() => const AccountManagementPage());
            },
          ),

          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
