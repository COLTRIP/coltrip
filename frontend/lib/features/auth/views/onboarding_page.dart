import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/storage/onboarding_storage.dart';
import '../../../shared/widgets/primary_button.dart';

class OnboardingPage extends StatefulWidget {
  const OnboardingPage({super.key});

  @override
  State<OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<OnboardingPage> {
  static const _storage = OnboardingStorage();
  final _pageController = PageController();
  int _currentPage = 0;
  bool _isFinishing = false;

  static const _pages = <_OnboardingContent>[
    _OnboardingContent(
      icon: Icons.waves_rounded,
      accentColor: Color(0xFF589C7E),
      backgroundColor: Color(0xFFE7F2ED),
      title: '북적이는 부산에서,\n나만의 고요를 찾아보세요',
      description: '유명 관광지 너머,\n조용히 머물기 좋은 장소를 소개해드려요.',
    ),
    _OnboardingContent(
      icon: Icons.auto_awesome_rounded,
      accentColor: Color(0xFFDAA95D),
      backgroundColor: Color(0xFFFFF3DF),
      title: '내 취향에 맞는\n콜드 플레이스를 만나보세요',
      description: '원하는 장소와 여행 감성에 맞춰\n나에게 어울리는 장소를 추천해드려요.',
    ),
    _OnboardingContent(
      icon: Icons.explore_rounded,
      accentColor: Color(0xFF5D85A6),
      backgroundColor: Color(0xFFE8F1F7),
      title: '조금 더 여유롭게,\n부산을 여행해보세요',
      description: '사람이 몰리는 곳을 벗어나\n부산의 새로운 매력을 발견해보세요.',
    ),
  ];

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  Future<void> _onPrimaryPressed() async {
    if (_currentPage < _pages.length - 1) {
      await _pageController.nextPage(
        duration: const Duration(milliseconds: 280),
        curve: Curves.easeOutCubic,
      );
      return;
    }

    if (_isFinishing) return;
    setState(() => _isFinishing = true);
    await _storage.markCompleted();
    if (mounted) {
      Get.offAllNamed(
        AppRoutes.locationPermission,
        arguments: AppRoutes.login,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isLastPage = _currentPage == _pages.length - 1;

    return Scaffold(
      backgroundColor: const Color(0xFFF7F9F8),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
          child: Column(
            children: [
              Align(
                alignment: Alignment.centerLeft,
                child: Image.asset(
                  'assets/images/logo.png',
                  width: 92,
                  fit: BoxFit.contain,
                ),
              ),
              Expanded(
                child: PageView.builder(
                  controller: _pageController,
                  itemCount: _pages.length,
                  onPageChanged: (index) {
                    setState(() => _currentPage = index);
                  },
                  itemBuilder: (context, index) {
                    return _OnboardingSlide(content: _pages[index]);
                  },
                ),
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(
                  _pages.length,
                  (index) => AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    width: index == _currentPage ? 24 : 8,
                    height: 8,
                    margin: const EdgeInsets.symmetric(horizontal: 4),
                    decoration: BoxDecoration(
                      color: index == _currentPage
                          ? const Color(0xFF589C7E)
                          : const Color(0xFFD7DDDA),
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 28),
              PrimaryButton(
                label: isLastPage ? 'COLTRIP 시작하기' : '다음',
                icon: isLastPage ? Icons.arrow_forward_rounded : null,
                isLoading: _isFinishing,
                onPressed: _onPrimaryPressed,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _OnboardingSlide extends StatelessWidget {
  const _OnboardingSlide({required this.content});

  final _OnboardingContent content;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return SingleChildScrollView(
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: constraints.maxHeight),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  width: 230,
                  height: 230,
                  decoration: BoxDecoration(
                    color: content.backgroundColor,
                    shape: BoxShape.circle,
                  ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      Icon(content.icon, size: 104, color: content.accentColor),
                      Positioned(
                        top: 42,
                        right: 38,
                        child: Icon(
                          Icons.circle,
                          size: 14,
                          color: content.accentColor.withValues(alpha: 0.35),
                        ),
                      ),
                      Positioned(
                        bottom: 48,
                        left: 38,
                        child: Icon(
                          Icons.circle,
                          size: 9,
                          color: content.accentColor.withValues(alpha: 0.25),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 52),
                Text(
                  content.title,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 25,
                    height: 1.35,
                    fontWeight: FontWeight.w700,
                    color: Color(0xFF252B28),
                  ),
                ),
                const SizedBox(height: 18),
                Text(
                  content.description,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 15,
                    height: 1.6,
                    fontWeight: FontWeight.w400,
                    color: Color(0xFF747C78),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _OnboardingContent {
  const _OnboardingContent({
    required this.icon,
    required this.accentColor,
    required this.backgroundColor,
    required this.title,
    required this.description,
  });

  final IconData icon;
  final Color accentColor;
  final Color backgroundColor;
  final String title;
  final String description;
}
