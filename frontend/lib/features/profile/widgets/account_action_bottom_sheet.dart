import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../shared/widgets/primary_button.dart';

/// 계정 관련 작업을 확인하는 공통 바텀시트입니다.
///
/// 로그아웃 및 회원 탈퇴와 같이 사용자 확인이 필요한 작업에 사용되며, 확인 및 취소 동작을 제공합니다.
class AccountActionBottomSheet extends StatelessWidget {
  const AccountActionBottomSheet({
    super.key,
    required this.icon,
    required this.message,
    required this.confirmText,
    required this.onConfirm,
    this.confirmColor = const Color(0xFF5CA887),
  });

  final IconData icon;
  final String message;
  final String confirmText;
  final VoidCallback onConfirm;
  final Color confirmColor;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.fromLTRB(28, 36, 28, 28),
        decoration: const BoxDecoration(
          color: Color(0xFFF7F9F8),
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 62,
              height: 62,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: const Color(0xFF252B28), width: 1),
              ),
              child: Icon(icon, size: 32, color: const Color(0xFF252B28)),
            ),

            const SizedBox(height: 18),

            Text(
              message,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w500,
                height: 1.4,
                color: Color(0xFF252B28),
              ),
            ),

            const SizedBox(height: 28),

            PrimaryButton(
              label: confirmText,
              onPressed: onConfirm,
              buttonColor: confirmColor,
            ),
            const SizedBox(height: 12),
            PrimaryButton(
              label: '취소',
              isOutlined: true,
              buttonColor: const Color(0xFF252B28),
              onPressed: () => Get.back(),
            ),
          ],
        ),
      ),
    );
  }
}

void showAccountActionBottomSheet({
  required IconData icon,
  required String message,
  required String confirmText,
  required VoidCallback onConfirm,
  Color confirmColor = const Color(0xFF5CA887),
}) {
  Get.bottomSheet(
    AccountActionBottomSheet(
      icon: icon,
      message: message,
      confirmText: confirmText,
      confirmColor: confirmColor,
      onConfirm: onConfirm,
    ),
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    barrierColor: Colors.black.withValues(alpha: 0.6),
  );
}
