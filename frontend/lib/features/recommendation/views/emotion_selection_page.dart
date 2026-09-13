import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../widgets/recommendation_selection_app_bar.dart';


class EmotionSelectionPage extends StatefulWidget {
  /// 장소선택에서 넘어온 백엔드 category enum 값 (예: 'CAFE'). 없으면 전체.
  final String? category;

  const EmotionSelectionPage({super.key, this.category});

  @override
  State<EmotionSelectionPage> createState() => _EmotionSelectionPageState();
}

class _EmotionSelectionPageState extends State<EmotionSelectionPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: RecommendationAppBar(
        action: RecommendationAppBarAction.skip,
        onPressed: () {
          Get.toNamed(AppRoutes.recommendationList, arguments: widget.category);
        },
      ),
      body: const Center(
        child: Column(
          children: [
            SizedBox(height: 100),

            Text(
              '원하시는 여행 감성을',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w600,
                fontSize: 20,
              ),
            ),
            Text(
              '선택해주세요 💫',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w600,
                fontSize: 20,
              ),
            ),
            Text(
              '*필수는 아니지만, 사용자님에게 맞는 관광지를',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w300,
                fontSize: 12,
              ),
            ),
            Text(
              '추천드리기 위해 선택해주세요!',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w300,
                fontSize: 12,
              ),
            ),

            // TODO: 감성 카테고리 픽스되는대로 추가
          ],
        ),
      ),
    );
  }
}
