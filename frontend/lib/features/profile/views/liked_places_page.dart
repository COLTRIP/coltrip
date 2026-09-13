import 'package:flutter/material.dart';

import '../../../shared/widgets/shared_app_bar.dart';

class LikedPlacesPage extends StatelessWidget {
  const LikedPlacesPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Column(
        children: [SharedAppBar(title: '좋아요한 장소', showBackButton: true)],
      ),
    );
  }
}
