import 'package:flutter/material.dart';

import '../../../models/review.dart';
import 'review_tile.dart';

class ReviewSection extends StatelessWidget {
  final List<SpotReview> reviews;
  final int previewCount;
  final VoidCallback? onSeeAllPressed;

  const ReviewSection({
    super.key,
    required this.reviews,
    this.previewCount = 2,
    this.onSeeAllPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              '리뷰',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Colors.black,
              ),
            ),
            if (reviews.isNotEmpty)
              GestureDetector(
                onTap: onSeeAllPressed,
                child: const Text(
                  '전체보기',
                  style: TextStyle(fontSize: 10, color: Color(0xFF7C7C7C)),
                ),
              ),
          ],
        ),
        if (reviews.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 22),
            child: Center(
              child: Text(
                '아직 등록된 리뷰가 없어요.',
                style: TextStyle(fontSize: 14, color: Color(0xFF8A918E)),
              ),
            ),
          )
        else
          for (final review in reviews.take(previewCount)) ...[
            const Divider(height: 24, color: Color(0x33252B28)),
            ReviewTile(review: review),
          ],
      ],
    );
  }
}
