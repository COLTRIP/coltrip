import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../models/place_review.dart';
import '../models/review_edit_result.dart';

/// 작성한 리뷰의 별점과 내용을 수정하는 다이얼로그를 표시합니다.
///
/// 수정이 완료되면 변경된 리뷰 정보를 반환하며, 취소한 경우 null을 반환합니다.
Future<ReviewEditResult?> showReviewEditDialog({
  required PlaceReview review,
}) async {
  var selectedRating = review.rating.round();
  final contentController = TextEditingController(text: review.content);

  final result = await Get.dialog<ReviewEditResult>(
    StatefulBuilder(
      builder: (context, setDialogState) {
        return AlertDialog(
          title: const Text(
            '리뷰 수정',
            style: TextStyle(fontWeight: FontWeight.w600),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(5, (index) {
                  final rating = index + 1;

                  return IconButton(
                    onPressed: () {
                      setDialogState(() {
                        selectedRating = rating;
                      });
                    },
                    icon: Icon(
                      rating <= selectedRating
                          ? Icons.star_rounded
                          : Icons.star_outline_rounded,
                      color: const Color(0xFF589C7E),
                      size: 32,
                    ),
                  );
                }),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: contentController,
                maxLength: 300,
                maxLines: 4,
                decoration: InputDecoration(
                  hintText: '리뷰를 작성해주세요.',
                  filled: true,
                  fillColor: const Color(0xFFF7F9F8),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Get.back<ReviewEditResult>(),
              child: const Text('취소'),
            ),
            TextButton(
              onPressed: () {
                Get.back<ReviewEditResult>(
                  result: ReviewEditResult(
                    rating: selectedRating,
                    content: contentController.text,
                  ),
                );
              },
              child: const Text(
                '완료',
                style: TextStyle(
                  color: Color(0xFF589C7E),
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        );
      },
    ),
  );

  contentController.dispose();
  return result;
}
