import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../shared/widgets/shared_app_bar.dart';
import '../controllers/profile_controller.dart';
import '../models/place.dart';
import '../models/place_review.dart';
import '../services/review_service.dart';
import '../widgets/place_detail_bottom_sheet.dart';
import '../widgets/place_list_card.dart';

class VisitedPlacesPage extends StatefulWidget {
  const VisitedPlacesPage({super.key});

  @override
  State<VisitedPlacesPage> createState() => _VisitedPlacesPageState();
}

class _VisitedPlacesPageState extends State<VisitedPlacesPage> {
  late final ProfileController _controller;
  final ReviewService _reviewService = ReviewService();

  @override
  void initState() {
    super.initState();
    _controller = Get.isRegistered<ProfileController>()
        ? Get.find<ProfileController>()
        : Get.put(ProfileController());
    _controller.loadVisitedPlaces();
  }

  Future<void> _openPlace(Place place) async {
    final spotId = place.id;
    if (spotId == null) return;

    PlaceReview? review;

    if (place.reviewId != null) {
      try {
        review = await _reviewService.getReviewBySpot(
          spotId: spotId,
          reviewId: place.reviewId!,
        );
      } on ReviewException catch (error) {
        Get.snackbar(
          '리뷰 조회 실패',
          error.message,
          snackPosition: SnackPosition.BOTTOM,
        );
        return;
      }
    }

    if (!mounted) return;

    showPlaceDetailBottomSheet(
      place: place,
      review: review,
      onEditReview: review == null ? null : () => _editReview(review!),
      onDeleteReview: review == null ? null : () => _deleteReview(review!),
    );
  }

  Future<void> _editReview(PlaceReview review) async {
    final result = await showReviewEditDialog(review: review);
    if (result == null) return;

    try {
      await _reviewService.updateReview(
        reviewId: review.id,
        rating: result['rating'] as int,
        content: result['content'] as String?,
      );

      Get.back();
      Get.snackbar(
        '수정 완료',
        '리뷰가 수정되었습니다.',
        snackPosition: SnackPosition.BOTTOM,
        backgroundColor: const Color(0xFFDDEFE7),
        colorText: const Color(0xFF252B28),
      );
      await _controller.loadVisitedPlaces();
    } on ReviewException catch (error) {
      Get.snackbar('수정 실패', error.message, snackPosition: SnackPosition.BOTTOM);
    }
  }

  Future<void> _deleteReview(PlaceReview review) async {
    final shouldDelete = await Get.dialog<bool>(
      AlertDialog(
        title: const Text('리뷰 삭제'),
        content: const Text('작성한 리뷰를 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Get.back(result: false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Get.back(result: true),
            child: const Text('삭제', style: TextStyle(color: Color(0xFFE15D5D))),
          ),
        ],
      ),
    );

    if (shouldDelete != true) return;

    try {
      await _reviewService.deleteReview(reviewId: review.id);

      Get.back();
      Get.snackbar(
        '삭제 완료',
        '리뷰가 삭제되었습니다.',
        snackPosition: SnackPosition.BOTTOM,
      );
      await _controller.loadVisitedPlaces();
    } on ReviewException catch (error) {
      Get.snackbar('삭제 실패', error.message, snackPosition: SnackPosition.BOTTOM);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const SharedAppBar(title: '방문한 장소', showBackButton: true),
      body: Obx(() {
        if (_controller.isVisitedPlacesLoading.value &&
            _controller.visitedPlaces.isEmpty) {
          return const Center(child: CircularProgressIndicator());
        }

        if (_controller.visitedPlaces.isEmpty) {
          return const Center(
            child: Text(
              '아직 방문한 장소가 없어요.',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                color: Color(0xFF7C8581),
              ),
            ),
          );
        }

        return RefreshIndicator(
          onRefresh: _controller.loadVisitedPlaces,
          child: ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: _controller.visitedPlaces.length,
            separatorBuilder: (_, __) => const SizedBox(height: 16),
            itemBuilder: (context, index) {
              final place = _controller.visitedPlaces[index];
              return PlaceListCard(
                place: place,
                onTap: () => _openPlace(place),
              );
            },
          ),
        );
      }),
    );
  }
}
