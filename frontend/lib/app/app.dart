import 'package:coltrip/app/navigation/main_shell.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

import 'routes/app_pages.dart';
import 'routes/app_routes.dart';


class ColtripApp extends StatelessWidget {
  const ColtripApp({super.key});

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      home: MainShell(),
      getPages: AppPages.pages,

      title: 'COLTRIP',
      debugShowCheckedModeBanner: false,
      initialRoute: AppRoutes.profile,
      // getPages: AppPages.pages,
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFFF7F9F8),
      ),
    );
  }
}