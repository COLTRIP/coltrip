import 'package:flutter/material.dart';
import 'package:get/get.dart';

import 'routes/app_pages.dart';
import 'routes/app_routes.dart';

/// COLTRIP 앱의 최상위 위젯입니다.
///
/// 전역 테마, 초기 화면 및 GetX 라우팅 설정을 관리합니다.
class ColtripApp extends StatelessWidget {
  const ColtripApp({super.key});

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      title: 'COLTRIP',
      debugShowCheckedModeBanner: false,
      initialRoute: AppRoutes.login,
      getPages: AppPages.pages,
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFFF7F9F8),
        fontFamily: 'Paperlogy',
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF589C7E),
        ),
      ),
    );
  }
}
