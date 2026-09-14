import 'package:flutter/material.dart';
import 'package:get/get.dart';

/// 앱 전반에서 공통으로 사용하는 제목형 앱 바입니다.
///
/// 선택적으로 뒤로가기 버튼과 사용자 지정 배경색을 지원합니다.
class SharedAppBar extends StatelessWidget
    implements PreferredSizeWidget {
  const SharedAppBar({
    super.key,
    required this.title,
    this.showBackButton = false,
    this.onBackPressed,
    this.backgroundColor = const Color(0xFFF8F9FA),
  });

  final String title;
  final bool showBackButton;
  final VoidCallback? onBackPressed;
  final Color backgroundColor;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      automaticallyImplyLeading: false,
      centerTitle: true,
      elevation: 0,
      scrolledUnderElevation: 0,
      backgroundColor: backgroundColor,
      surfaceTintColor: Colors.transparent,
      title: Text(
        title,
        style: const TextStyle(
          color: Color(0xFF252B28),
          fontSize: 20,
          fontWeight: FontWeight.w600,
        ),
      ),
      leading: showBackButton
          ? IconButton(
        onPressed: onBackPressed ?? () => Get.back(),
        icon: const Icon(
          Icons.chevron_left,
          size: 32,
          color: Color(0xFF252B28),
        ),
      )
          : null,
    );
  }
}
