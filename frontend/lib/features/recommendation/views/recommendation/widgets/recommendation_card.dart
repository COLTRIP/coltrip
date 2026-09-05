import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../models/recommendation.dart';
import '../../recommendation_detail/recommendation_detail_page.dart';

class RecommendationCard extends StatelessWidget {
  final Spot spot;

  const RecommendationCard({super.key, required this.spot});

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.sizeOf(context).height;
    final cardHeight = (screenHeight * 0.15).clamp(100.0, 150.0); //카드 높이
    return InkWell(
      onTap: () => Get.to(() => RecommendationDetailPage(spotId: spot.id)),
      borderRadius: BorderRadius.circular(10),
      child: Container(
        height: cardHeight,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(10),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.05),
              blurRadius: 7,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ClipRRect(
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(10),
                bottomLeft: Radius.circular(10),
              ),
              child: Image.network(
                spot.imageUrl,
                width: 140,
                fit: BoxFit.cover,
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(12, 8, 12, 13),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          spot.name,
                          style: const TextStyle(
                            fontFamily: 'Paperlogy',
                            fontSize: 18,
                            fontWeight: FontWeight.w500,
                            color: Colors.black,
                          ),
                        ),
                        Text(
                          spot.address,
                          style: const TextStyle(
                            fontFamily: 'Paperlogy',
                            fontSize: 10,
                            fontWeight: FontWeight.w300,
                            color: Color(0xFF7C7C7C),
                          ),
                        ),
                      ],
                    ),
                    _QuietScoreBar(quietScore: spot.quietScore),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _QuietScoreBar extends StatelessWidget {
  final int quietScore;

  const _QuietScoreBar({required this.quietScore});

  @override
  Widget build(BuildContext context) {
    final ratio = quietScore.clamp(0, 100) / 100;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              '고요지수',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontSize: 10,
                fontWeight: FontWeight.w500,
                color: Colors.black,
              ),
            ),
            Text(
              '$quietScore점',
              style: const TextStyle(
                fontFamily: 'Paperlogy',
                fontSize: 10,
                fontWeight: FontWeight.w500,
                color: Colors.black,
              ),
            ),
          ],
        ),
        const SizedBox(height: 3),
        SizedBox(
          height: 7,
          child: ClipRRect(
            borderRadius: BorderRadius.circular(10),
            child: SizedBox(
              height: 7,
              child: Stack(
                children: [
                  Container(color: const Color(0xFFD9D9D9)),
                  FractionallySizedBox(
                    widthFactor: ratio,
                    child: Container(color: const Color(0xFF589C7E)),
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}
