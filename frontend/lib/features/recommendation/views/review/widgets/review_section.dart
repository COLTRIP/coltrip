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
                fontFamily: 'Paperlogy',
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Colors.black,
              ),
            ),
            GestureDetector(
              onTap: onSeeAllPressed,
              child: const Text(
                '전체보기',
                style: TextStyle(fontFamily: 'Paperlogy', fontSize: 10, color: Color(0xFF7C7C7C)),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        for (final review in reviews.take(previewCount)) ...[
          const Divider(height: 24, color: Color(0x33252B28)),
          ReviewTile(review: review),
        ],
      ],
    );
  }
}
