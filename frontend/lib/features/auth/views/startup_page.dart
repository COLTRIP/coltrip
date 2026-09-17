import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/storage/api_environment_storage.dart';
import '../../../core/storage/onboarding_storage.dart';
import '../../../core/storage/token_storage.dart';

/// 앱 실행 시 로그인 토큰과 온보딩 완료 여부를 확인해 첫 화면을 결정합니다.
class StartupPage extends StatefulWidget {
  const StartupPage({super.key});

  @override
  State<StartupPage> createState() => _StartupPageState();
}

class _StartupPageState extends State<StartupPage> {
  static const _tokenStorage = TokenStorage();
  static const _onboardingStorage = OnboardingStorage();
  static const _environmentStorage = ApiEnvironmentStorage();

  @override
  void initState() {
    super.initState();
    _resolveInitialRoute();
  }

  Future<void> _resolveInitialRoute() async {
    final results = await Future.wait([
      _tokenStorage.readAccessToken(),
      _tokenStorage.readRefreshToken(),
      _onboardingStorage.hasCompleted(),
      _environmentStorage.isDemoMode(),
    ]);
    if (!mounted) return;

    final hasAccessToken = (results[0] as String?)?.isNotEmpty ?? false;
    final hasRefreshToken = (results[1] as String?)?.isNotEmpty ?? false;
    final hasCompletedOnboarding = results[2] as bool;
    final isDemoMode = results[3] as bool;
    DioClient.configureEnvironment(demoMode: isDemoMode);

    if (hasAccessToken || hasRefreshToken) {
      Get.offAllNamed(AppRoutes.main);
    } else if (!hasCompletedOnboarding) {
      Get.offAllNamed(AppRoutes.onboarding);
    } else {
      Get.offAllNamed(AppRoutes.login);
    }
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(child: CircularProgressIndicator(color: Color(0xFF589C7E))),
    );
  }
}
