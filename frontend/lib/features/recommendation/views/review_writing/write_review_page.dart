import 'package:coltrip/features/recommendation/views/review_writing/widget/text_review.dart';
import 'package:coltrip/shared/widgets/app_button.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../models/recommendation.dart';
import '../visiting_spot/widgets/visiting_spot_card.dart';
import 'widget/satisfaction_section.dart';

class WriteReviewPage extends StatelessWidget {
  final SpotDetail spot;

  const WriteReviewPage({super.key, required this.spot});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20,vertical: 16),
        child: Column(
          children: [
            Align(
              alignment: AlignmentGeometry.topLeft,
              child: IconButton(
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
                onPressed: () => Get.back(),
                icon: const Icon(Icons.arrow_back, color: Colors.black),
              ),
            ),
            const SizedBox(height: 10),
            VisitingSpotCard(spot: spot),
            const SizedBox(height: 30),
            const Text(
              '방문했던 곳, 어떠셨나요?\n리뷰를 남겨주세요!',
              style: TextStyle(
                fontWeight: FontWeight.w600,
                fontFamily: 'Paperlogy',
                fontSize: 16,
              ),
              textAlign: TextAlign.center,
            ),const SizedBox(height: 20,),
            const Divider(height: 24, color: Color(0x33252B28)),
            const SizedBox(height: 16),
            const SatisfactionSection(),
            const SizedBox(height: 32),
            const TextReview(),
            const SizedBox(height: 32,),
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: PrimaryButton(label: '작성 완료', onPressed: () {
                Get.back();
                //TODO : 작성완료 로직 작성
              },),
            )
          ],
        ),
      ),
    );
  }
}
