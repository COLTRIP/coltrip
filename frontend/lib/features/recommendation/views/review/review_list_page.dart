import 'package:flutter/material.dart';

import '../../models/review.dart';
import 'widgets/review_tile.dart';

class ReviewListPage extends StatelessWidget {
  final List<SpotReview> reviews;

  const ReviewListPage({super.key, required this.reviews});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('리뷰 전체보기', style: TextStyle(fontFamily: 'Paperlogy', fontSize: 16, fontWeight: FontWeight.w500),)),
      body: SafeArea(
        top: false,
        child: reviews.isEmpty
            ? const Center(child: Text('아직 리뷰가 없어요.'))
            : ListView.separated(
                padding: const EdgeInsets.all(24),
                itemCount: reviews.length,
                separatorBuilder: (context, index) => const Divider(height: 24, color: Color(0x33252B28)),
                itemBuilder: (context, index) => ReviewTile(review: reviews[index]),
              ),
      ),
    );
  }
}
