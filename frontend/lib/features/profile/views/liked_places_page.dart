import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../shared/widgets/shared_app_bar.dart';
import '../controllers/profile_controller.dart';
import '../widgets/place_list_card.dart';

/// 사용자가 좋아요한 장소 목록을 확인하는 화면입니다.
///
/// 좋아요한 장소를 목록으로 표시하고, 새로고침을 통해 최신 목록을 다시 불러올 수 있습니다.
class LikedPlacesPage extends StatefulWidget {
  const LikedPlacesPage({super.key});

  @override
  State<LikedPlacesPage> createState() => _LikedPlacesPageState();
}

class _LikedPlacesPageState extends State<LikedPlacesPage> {
  late final ProfileController _controller;

  @override
  void initState() {
    super.initState();
    _controller = Get.find<ProfileController>();
    _controller.loadLikedPlaces();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const SharedAppBar(title: '좋아요한 장소', showBackButton: true),
      body: Obx(() {
        if (_controller.isLikedPlacesLoading.value &&
            _controller.likedPlaces.isEmpty) {
          return const Center(child: CircularProgressIndicator());
        }

        if (_controller.likedPlaces.isEmpty) {
          return const Center(
            child: Text(
              '아직 좋아요한 장소가 없어요.',
              style: TextStyle(
                color: Color(0xFF7C8581),
              ),
            ),
          );
        }

        return RefreshIndicator(
          onRefresh: _controller.loadLikedPlaces,
          child: ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: _controller.likedPlaces.length,
            separatorBuilder: (_, __) => const SizedBox(height: 16),
            itemBuilder: (context, index) {
              return PlaceListCard(place: _controller.likedPlaces[index]);
            },
          ),
        );
      }),
    );
  }
}
