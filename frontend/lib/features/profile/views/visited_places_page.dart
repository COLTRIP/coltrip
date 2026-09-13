import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../data/dummy_place_reviews.dart';
import '../../../data/dummy_places.dart';
import '../../../shared/widgets/shared_app_bar.dart';
import '../services/review_service.dart';
import '../widgets/place_list_card.dart';
import '../widgets/place_detail_bottom_sheet.dart';

class VisitedPlacesPage extends StatelessWidget {
  const VisitedPlacesPage({super.key});

  @override
  Widget build(BuildContext context) {
    final reviewService = ReviewService();

    return Scaffold(
      appBar: const SharedAppBar(title: '방문한 장소', showBackButton: true),
      body: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: visitedPlaces.length,
        separatorBuilder: (_, __) {
          return const SizedBox(height: 16);
        },
        itemBuilder: (context, index) {
          final place = visitedPlaces[index];

          final review = place.id == null ? null : dummyPlaceReviews[place.id];

          return PlaceListCard(
            place: place,
            onTap: () {
              showPlaceDetailBottomSheet(
                place: place,
                review: review,
                onEditReview: review == null
                    ? null
                    : () async {
                        final result = await showReviewEditDialog(
                          review: review,
                        );

                        if (result == null) {
                          return;
                        }

                        try {
                          final reviewService = ReviewService();

                          await reviewService.updateReview(
                            reviewId: review.id,
                            rating: result['rating'] as int,
                            content: result['content'] as String?,
                          );

                          // 장소 상세 바텀시트 닫기
                          Get.back();

                          Get.snackbar(
                            '수정 완료',
                            '리뷰가 수정되었습니다.',
                            snackPosition: SnackPosition.BOTTOM,
                            backgroundColor: const Color(0xFFDDEFE7),
                            colorText: const Color(0xFF252B28),
                          );

                          // TODO: 실제 API 연결 후 방문 목록 다시 조회
                        } on ReviewException catch (error) {
                          Get.snackbar(
                            '수정 실패',
                            error.message,
                            snackPosition: SnackPosition.BOTTOM,
                          );
                        }
                      },
                onDeleteReview: review == null
                    ? null
                    : () async {
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
                                child: const Text(
                                  '삭제',
                                  style: TextStyle(color: Color(0xFFE15D5D)),
                                ),
                              ),
                            ],
                          ),
                        );

                        if (shouldDelete != true) {
                          return;
                        }

                        try {
                          await reviewService.deleteReview(reviewId: review.id);

                          // 상세 바텀시트 닫기
                          Get.back();

                          Get.snackbar(
                            '삭제 완료',
                            '리뷰가 삭제되었습니다.',
                            snackPosition: SnackPosition.BOTTOM,
                          );

                          // TODO: 방문 목록 및 리뷰 다시 조회
                        } on ReviewException catch (error) {
                          Get.snackbar(
                            '삭제 실패',
                            error.message,
                            snackPosition: SnackPosition.BOTTOM,
                          );
                        }
                      },
              );
            },
          );
        },
      ),
    );
  }
}
