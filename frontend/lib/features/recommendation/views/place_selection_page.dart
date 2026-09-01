import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../features/recommendation/widgets/recommendation_selection_app_bar.dart';
import '../../../app/routes/app_routes.dart';
import '../data/place_type_data.dart';
import '../../../features/recommendation/widgets/place_type_grid.dart';
import '../models/place_type_item.dart';


class RecommendationPage extends StatefulWidget {
  const RecommendationPage({super.key});

  @override
  State<RecommendationPage> createState() => _RecommendationPageState();
}

class _RecommendationPageState extends State<RecommendationPage> {
  String? selectedPlaceId;

  void selectPlace(PlaceTypeItem item) {
    setState(() {
      selectedPlaceId = item.id;
    });
  }

  void moveToEmotionSelection() {
    if (selectedPlaceId == null) {
      return;
    }

    Get.toNamed(
      AppRoutes.emotionSelection,
      arguments: {
        'placeId': selectedPlaceId,
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: RecommendationAppBar(
        showBackButton: false,
        onPressed: () {
          Get.toNamed(AppRoutes.emotionSelection);
        },
      ),
      body: Column(
        children: [
          const SizedBox(height: 20),

          const Text(
            '원하시는 장소 유형을',
            style: TextStyle(
              fontFamily: 'Paperlogy',
              fontWeight: FontWeight.w600,
              fontSize: 20,
            ),
          ),
          const Text(
            '선택해주세요📍',
            style: TextStyle(
              fontFamily: 'Paperlogy',
              fontWeight: FontWeight.w600,
              fontSize: 20,
            ),
          ),

          const SizedBox(height: 20),

          Expanded(
            child: PlaceTypeGrid(
                items: PlaceTypeData.items,
                selectedId: selectedPlaceId,
                onSelected: selectPlace,
            ),
          ),
        ],
      ),
    );
  }
}
