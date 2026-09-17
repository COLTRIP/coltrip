import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../shared/widgets/shared_app_bar.dart';
import '../../auth/services/google_auth_service.dart';
import '../controllers/profile_controller.dart';
import '../widgets/account_action_bottom_sheet.dart';
import '../widgets/account_menu_button.dart';

/// 사용자 계정 관련 기능을 관리하는 화면입니다.
///
/// 닉네임 변경, 로그아웃 및 회원 탈퇴 기능을 제공합니다.
class AccountManagementPage extends StatelessWidget {
  const AccountManagementPage({super.key});

  @override
  Widget build(BuildContext context) {
    final profileController = Get.find<ProfileController>();

    return Scaffold(
      appBar: const SharedAppBar(title: '계정 관리', showBackButton: true),
      body: Column(
        children: [
          AccountMenuButton(
            title: '닉네임 변경',
            onTap: () async {
              final result = await Get.toNamed(
                AppRoutes.changeNickname,
                arguments: profileController.nickname.value,
              );

              if (result is String) {
                profileController.nickname.value = result;
              }
            },
          ),

          AccountMenuButton(
            title: '로그아웃',
            onTap: () {
              showAccountActionBottomSheet(
                icon: Icons.logout,
                message: '현재 계정에서 로그아웃하시겠습니까?\n언제든지 다시 로그인할 수 있습니다.',
                confirmText: '로그아웃',
                onConfirm: () async {
                  try {
                    final authService = GoogleAuthService();

                    await authService.logout();

                    Get.back();
                    Get.offAllNamed(AppRoutes.login);
                  } on AuthException catch (error) {
                    Get.back();

                    Get.snackbar(
                      '로그아웃 실패',
                      error.message,
                      snackPosition: SnackPosition.BOTTOM,
                    );
                  }
                },
              );
            },
          ),

          AccountMenuButton(
            title: '탈퇴',
            isDanger: true,
            onTap: () {
              showAccountActionBottomSheet(
                icon: Icons.person_off_outlined,
                message: '탈퇴 후 계정 복구는 불가합니다.\n정말로 탈퇴하시겠습니까?',
                confirmText: '탈퇴',
                confirmColor: const Color(0xFFC96363),
                onConfirm: () async {
                  try {
                    await GoogleAuthService().deleteAccount();

                    Get.back();
                    Get.offAllNamed(AppRoutes.login);

                    Get.snackbar(
                      '탈퇴 완료',
                      '회원 탈퇴가 완료되었습니다.',
                      snackPosition: SnackPosition.BOTTOM,
                    );
                  } on AuthException catch (error) {
                    Get.back();

                    Get.snackbar(
                      '탈퇴 실패',
                      error.message,
                      snackPosition: SnackPosition.BOTTOM,
                    );
                  }
                },
              );
            },
          ),
        ],
      ),
    );
  }
}
