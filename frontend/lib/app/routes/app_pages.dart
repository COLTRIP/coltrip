import 'package:get/get.dart';

import '../../features/recommendation/models/recommendation.dart';
import '../../features/recommendation/models/review.dart';
import '../../features/recommendation/views/location_permission/location_permission_page.dart';
import '../../features/recommendation/views/recommendation/recommendation_page.dart';
import '../../features/recommendation/views/recommendation_detail/recommendation_detail_page.dart';
import '../../features/recommendation/views/review/review_list_page.dart';
import '../../features/recommendation/views/review_writing/write_review_page.dart';
import '../../features/recommendation/views/visiting_spot/visiting_spot_page.dart';
import 'app_routes.dart';


abstract final class AppPages {
  static final pages = <GetPage<dynamic>>[
    GetPage(
      name: AppRoutes.recommendation,
      page: () => RecommendationPage(
        // TODO: 감성/카테고리 선택 화면에서 넘어온 값으로 교체
        category: Get.arguments as String? ?? '전체',
      ),
    ),
    GetPage(
      name: AppRoutes.recommendationDetail,
      page: () => RecommendationDetailPage(spotId: Get.arguments as int),
    ),
    GetPage(
      name: AppRoutes.visitingSpot,
      page: () => VisitingSpotPage(spot: Get.arguments as SpotDetail),
    ),
    GetPage(
      name: AppRoutes.reviewList,
      page: () => ReviewListPage(reviews: Get.arguments as List<SpotReview>),
    ),
    GetPage(
      name: AppRoutes.reviewWrite,
      page: () {
        final args = Get.arguments as Map<String, dynamic>;
        return WriteReviewPage(
          spot: args['spot'] as SpotDetail,
          visitId: args['visitId'] as int,
        );
      },
    ),
    GetPage(
      name: AppRoutes.locationPermission,
      page: () => const LocationPermissionPage(),
    ),
  ];
}
