import 'package:get/get.dart';

import '../../features/auth/views/login_page.dart';
import '../../features/auth/views/nickname_setup_page.dart';
import '../../features/map/views/map_page.dart';
import '../../features/profile/views/profile_page.dart';
import '../../features/recommendation/models/current_visit.dart';
import '../../features/recommendation/models/recommendation.dart';
import '../../features/recommendation/models/review.dart';
import '../../features/recommendation/views/emotion_selection_page.dart';
import '../../features/recommendation/views/location_permission/location_permission_page.dart';
import '../../features/recommendation/views/place_selection_page.dart';
import '../../features/recommendation/views/recommendation/recommendation_page.dart';
import '../../features/recommendation/views/recommendation_detail/recommendation_detail_page.dart';
import '../../features/recommendation/views/review/review_list_page.dart';
import '../../features/recommendation/views/review_writing/write_review_page.dart';
import '../../features/recommendation/views/visiting_spot/visiting_spot_page.dart';
import '../navigation/main_shell.dart';
import 'app_routes.dart';

abstract final class AppPages {
  static final pages = <GetPage<dynamic>>[
    GetPage(
      name: AppRoutes.main,
      page: () => MainShell(),
    ),
    GetPage(
      name: AppRoutes.login,
      page: () => const LoginPage(),
    ),
    GetPage(
      name: AppRoutes.nickname,
      page: () => const NicknameSetupPage(),
    ),
    GetPage(
      name: AppRoutes.map,
      page: () => const MapPage(),
    ),
    GetPage(
      name: AppRoutes.profile,
      page: () => const ProfilePage(),
    ),
    GetPage(
      name: AppRoutes.recommendation,
      page: () => const PlaceSelectionPage(),
    ),
    GetPage(
      name: AppRoutes.emotionSelection,
      page: () {
        final args = Get.arguments;
        final category = args is Map ? args['category'] as String? : null;
        return EmotionSelectionPage(category: category);
      },
    ),
    GetPage(
      name: AppRoutes.recommendationList,
      page: () => RecommendationPage(
        category: Get.arguments as String? ?? '전체',
      ),
    ),
    GetPage(
      name: AppRoutes.recommendationDetail,
      page: () => RecommendationDetailPage(spotId: Get.arguments as int),
    ),
    GetPage(
      name: AppRoutes.visitingSpot,
      page: () {
        final args = Get.arguments;
        if (args is Map) {
          return VisitingSpotPage(
            spot: args['spot'] as SpotDetail,
            resumeVisit: args['visit'] as CurrentVisit?,
          );
        }
        return VisitingSpotPage(spot: args as SpotDetail);
      },
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
