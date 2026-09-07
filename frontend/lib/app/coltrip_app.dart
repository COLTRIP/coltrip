import 'package:flutter/material.dart';
import 'package:get/get.dart';

import 'routes/app_pages.dart';
import 'routes/app_routes.dart';

class ColtripApp extends StatelessWidget {
  const ColtripApp({super.key});

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      title: 'COLTRIP',
      debugShowCheckedModeBanner: false,
      initialRoute: AppRoutes.recommendation,
      getPages: AppPages.pages,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFFF7F9F8)),
        scaffoldBackgroundColor: const Color(0xFFF7F9F8),
        fontFamily: 'Paperlogy',
      ),
    );
  }
}
