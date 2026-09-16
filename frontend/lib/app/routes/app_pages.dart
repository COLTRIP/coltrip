import 'package:get/get.dart';

import '../../features/auth/views/nickname_setup_page.dart';
import '../../features/auth/views/login_page.dart';
import '../../features/auth/views/onboarding_page.dart';
import '../../features/auth/views/startup_page.dart';
import '../../features/auth/views/terms_page.dart';
import '../../features/profile/views/change_nickname_page.dart';
import '../../features/profile/views/app_info_page.dart';
import '../../features/recommendation/models/current_visit.dart';
import '../../features/recommendation/models/recommendation.dart';
import '../../features/recommendation/models/recommendation_detail_arguments.dart';
import '../../features/recommendation/models/recommendation_page_arguments.dart';
import '../../features/recommendation/models/review.dart';
import '../../features/recommendation/views/location_permission/location_permission_page.dart';
import '../../features/recommendation/views/mood_selection_page.dart';
import '../../features/recommendation/views/recommendation/recommendation_page.dart';
import '../../features/recommendation/views/recommendation_detail/recommendation_detail_page.dart';
import '../../features/recommendation/views/review/review_list_page.dart';
import '../../features/recommendation/views/review_writing/write_review_page.dart';
import '../../features/recommendation/views/visiting_spot/visiting_spot_page.dart';
import '../bindings/main_shell_binding.dart';
import '../navigation/main_shell.dart';
import 'app_routes.dart';

/// 라우트 경로와 실제 화면을 연결하는 GetX 라우트 목록입니다.
///
/// 화면 생성 방식, 의존성 바인딩 및 화면 이동 argument 변환을 관리합니다.
abstract final class AppPages {
  static final pages = <GetPage<dynamic>>[
    GetPage(name: AppRoutes.startup, page: () => const StartupPage()),
    GetPage(name: AppRoutes.onboarding, page: () => const OnboardingPage()),
    GetPage(
      name: AppRoutes.main,
      page: () => const MainShell(),
      binding: MainShellBinding(),
    ),
    GetPage(name: AppRoutes.login, page: () => const LoginPage()),
    GetPage(name: AppRoutes.nickname, page: () => const NicknameSetupPage()),
    GetPage(
      name: AppRoutes.moodSelection,
      page: () => const MoodSelectionPage(),
    ),
    GetPage(
      name: AppRoutes.changeNickname,
      page: () {
        final args = Get.arguments;
        return ChangeNicknamePage(
          currentNickname: args is String ? args : null,
        );
      },
    ),
    GetPage(
      name: AppRoutes.recommendationList,
      page: () {
        // 이전 선택 화면에서 전달한 장소 유형과 감성 조건
        final args = Get.arguments as RecommendationPageArguments;

        return RecommendationPage(
          category: args.category,
          categoryLabel: args.categoryLabel,
          modes: args.modes,
        );
      },
    ),
    GetPage(
      name: AppRoutes.recommendationDetail,
      page: () {
        final args = Get.arguments;

        if (args is RecommendationDetailArguments) {
          return RecommendationDetailPage(
            spotId: args.spotId,
            predictedQuietScore: args.predictedQuietScore,
            predictionTargetAt: args.predictionTargetAt,
          );
        }

        return RecommendationDetailPage(spotId: args as int);
      },
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
    GetPage(name: AppRoutes.terms, page: () => const TermsPage()),
    GetPage(name: AppRoutes.info, page: () => const AppInfoPage()),
  ];
}
