import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../core/storage/token_storage.dart';
import '../../features/map/views/map_page.dart';
import '../../features/profile/views/profile_page.dart';
import '../../features/recommendation/repositories/visiting_spot_repository.dart';
import '../../features/recommendation/views/place_selection_page.dart';
import '../../shared/widgets/app_bottom_navigation_bar.dart';
import '../routes/app_routes.dart';
import 'main_navigation_controller.dart';

/// 지도, 추천, 프로필 탭을 유지하면서 전환하는 메인 화면입니다.
///
/// [IndexedStack]을 사용해 탭을 변경해도 각 화면의 상태가 유지됩니다.
class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  final _tokenStorage = const TokenStorage();
  final _visitingRepository = VisitingSpotRepository();
  bool _didCheckCurrentVisit = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _restoreCurrentVisit();
    });
  }

  Future<void> _restoreCurrentVisit() async {
    if (_didCheckCurrentVisit) return;
    _didCheckCurrentVisit = true;

    final accessToken = await _tokenStorage.readAccessToken();
    final refreshToken = await _tokenStorage.readRefreshToken();
    if (!mounted || (accessToken == null && refreshToken == null)) return;

    try {
      final current = await _visitingRepository.getCurrentVisit();
      if (!mounted || current == null || current.status != 'STARTED') return;
      if (Get.currentRoute != AppRoutes.main) return;

      Get.toNamed(
        AppRoutes.visitingSpot,
        arguments: {
          'spot': current.toPlaceholderSpotDetail(),
          'visit': current,
        },
      );
    } catch (error) {
      debugPrint('진행 중 방문 자동 복원 실패: $error');
    }
  }

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<MainNavigationController>();

    return Obx(() {
      final currentIndex = controller.currentIndex.value;

      return Scaffold(
        body: IndexedStack(
          index: currentIndex,
          children: [
            MapPage(isActive: currentIndex == 0),
            const PlaceSelectionPage(),
            const ProfilePage(),
          ],
        ),
        bottomNavigationBar: const AppBottomNavigationBar(),
      );
    });
  }
}
