import 'package:flutter/material.dart';

import '../../../models/review.dart';

class ReviewTile extends StatelessWidget {
  final SpotReview review;

  const ReviewTile({super.key, required this.review});

  String _formatDate(DateTime date) {
    return '${date.year}.${date.month.toString().padLeft(2, '0')}.${date.day.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Column(
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                const CircleAvatar(
                  radius: 24,
                  backgroundColor: Color(0xFFE5E5E5),
                  child: Icon(Icons.person, size: 32, color: Colors.white),
                ),
                const SizedBox(width: 12),
                Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      review.userName,
                      style: const TextStyle(
                        fontFamily: 'Paperlogy',
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(
                          Icons.star,
                          size: 14,
                          color: Color(0xFF589C7E),
                        ),
                        Text(
                          ' ${review.rating}',
                          style: const TextStyle(fontFamily: 'Paperlogy', fontSize: 12),
                        ),
                      ],
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 10),
            Align(
              alignment: AlignmentGeometry.centerLeft,
              child: Text(
                review.content,
                style: const TextStyle(
                  fontFamily: 'Paperlogy',
                  fontSize: 12,
                  fontWeight: FontWeight.w300,
                  color: Colors.black,
                ),
              ),
            ),
          ],
        ),
        Positioned(
          top: 0,
          right: 0,
          child: Text(
            _formatDate(review.date),
            style: const TextStyle(
              fontFamily: 'Paperlogy',
              fontSize: 10,
              color: Color(0xFF7C7C7C),
            ),
          ),
        ),
      ],
    );
  }
}
