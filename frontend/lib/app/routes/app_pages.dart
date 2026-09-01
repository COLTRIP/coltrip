import 'package:coltrip/features/auth/views/nickname_setup_page.dart';
import 'package:get/get.dart';

import '../../features/auth/views/login_page.dart';
import '../../features/map/views/map_page.dart';
import '../../features/profile/views/profile_page.dart';
import '../../features/recommendation/views/place_selection_page.dart';
import '../../features/recommendation/views/emotion_selection_page.dart';
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
      name: AppRoutes.recommendation,
      page: () => const RecommendationPage(),
    ),
    GetPage(
      name: AppRoutes.emotionSelection,
      page: () => const EmotionSelectionPage(),
    ),
    GetPage(
      name: AppRoutes.profile,
      page: () => const ProfilePage(),
    ),
  ];
}