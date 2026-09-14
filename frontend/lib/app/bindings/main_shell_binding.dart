import 'package:get/get.dart';

import '../../features/profile/controllers/profile_controller.dart';
import '../../features/recommendation/controllers/recommendation_selection_controller.dart';
import '../navigation/main_navigation_controller.dart';

/// 메인 탭 화면에서 공유할 GetX 컨트롤러를 등록합니다.
///
/// `fenix: true`로 등록된 컨트롤러는 제거된 이후 다시 요청되면
/// 새로운 인스턴스로 생성됩니다.
class MainShellBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<ProfileController>(
      () => ProfileController(),
      fenix: true,
    );

    Get.lazyPut<RecommendationSelectionController>(
      () => RecommendationSelectionController(),
      fenix: true,
    );

    Get.lazyPut<MainNavigationController>(
      () => MainNavigationController(),
    );
  }
}
