import 'package:flutter/material.dart';
import 'package:get/get.dart';


enum RecommendationAppBarAction {
  next,
  skip,
}

class RecommendationAppBar extends StatelessWidget
    implements PreferredSizeWidget {
  const RecommendationAppBar({
    super.key,
    required this.onPressed,
    this.onBackPressed,
    this.action = RecommendationAppBarAction.next,
    this.showBackButton = true,
    this.backgroundColor = const Color(0xFFF7F9F8),
  });

  final VoidCallback onPressed;
  final VoidCallback? onBackPressed;
  final RecommendationAppBarAction action;
  final bool showBackButton;
  final Color backgroundColor;

  bool get _isNext => action == RecommendationAppBarAction.next;

  @override
  Size get preferredSize => const Size.fromHeight(72);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      automaticallyImplyLeading: false,
      backgroundColor: backgroundColor,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      toolbarHeight: preferredSize.height,
      leadingWidth: 80,

      // 왼쪽 back 버튼
      leading: showBackButton
          ? Padding(
        padding: const EdgeInsets.only(left: 16),
        child: _ActionButton(
          icon: Icons.arrow_back,
          label: 'back',
          onTap: onBackPressed ?? Get.back,
        ),
      )
          : null,

      // 오른쪽 next 또는 skip 버튼
      actions: [
        Padding(
          padding: const EdgeInsets.only(right: 16),
          child: _ActionButton(
            icon: _isNext ? Icons.arrow_forward : Icons.skip_next,
            label: _isNext ? 'next' : 'skip',
            onTap: onPressed,
          ),
        ),
      ],
    );
  }
}

class _ActionButton extends StatelessWidget {
  const _ActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: 8,
          vertical: 8,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              icon,
              size: 28,
              color: Colors.black,
            ),
            Text(
              label,
              style: const TextStyle(
                color: Colors.black87,
                fontSize: 12,
                height: 1,
              ),
            ),
          ],
        ),
      ),
    );
  }
}