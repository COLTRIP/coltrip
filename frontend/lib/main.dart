import 'package:flutter/material.dart';
import 'package:get/get.dart';

import 'features/recommendation/views/recommendation/recommendation_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      title: 'Coltrip',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFFF7F9F8)),
        scaffoldBackgroundColor: const Color(0xFFF7F9F8),
        fontFamily: 'Paperlogy',
      ),
      // TODO: 카테고리 선택 화면 연결되면 실제 선택값으로 교체
      home: const RecommendationPage(category: '해변'),
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Coltrip')),
      body: const Center(child: Text('Coltrip')),
    );
  }
}
