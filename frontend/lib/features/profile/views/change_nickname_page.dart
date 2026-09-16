import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../core/network/api_exception.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../../shared/widgets/shared_app_bar.dart';
import '../services/profile_service.dart';

/// 사용자의 닉네임을 변경하는 화면입니다.
///
/// 현재 닉네임을 표시하고, 변경된 닉네임을 서버에 저장한 뒤 계정 관리 화면에 변경 결과를 전달합니다.
class ChangeNicknamePage extends StatefulWidget {
  const ChangeNicknamePage({super.key, this.currentNickname});

  final String? currentNickname;

  @override
  State<ChangeNicknamePage> createState() => _ChangeNicknamePageState();
}

class _ChangeNicknamePageState extends State<ChangeNicknamePage> {
  final TextEditingController _nicknameController = TextEditingController();

  final ProfileService _profileService = ProfileService();

  bool _isLoading = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();

    _nicknameController.text = widget.currentNickname ?? '';
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_isLoading) {
      return;
    }

    FocusScope.of(context).unfocus();

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    final nickname = _nicknameController.text.trim();

    try {
      await _profileService.updateNickname(nickname: nickname);

      if (!mounted) {
        return;
      }

      setState(() {
        _isLoading = false;
      });

      // 계정 관리 페이지로 돌아가면서 변경된 닉네임 전달
      Get.back(result: nickname);

      Get.snackbar(
        '변경 완료',
        '닉네임이 변경되었습니다.',
        snackPosition: SnackPosition.BOTTOM,
      );
    } on ApiException catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        _isLoading = false;
        _errorMessage = error.message;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      resizeToAvoidBottomInset: true,
      appBar: const SharedAppBar(title: '닉네임 변경', showBackButton: true),
      body: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 42, 20, 16),
          child: Column(
            children: [
              TextField(
                controller: _nicknameController,
                autofocus: true,
                textInputAction: TextInputAction.done,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w400,
                  color: Color(0xFF252B28),
                ),
                decoration: InputDecoration(
                  hintText: '변경하고 싶은 닉네임(1~20자리 한글, 영문 및 숫자)',
                  hintStyle: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w400,
                    color: Color(0xFF505653),
                  ),
                  errorText: _errorMessage,
                  contentPadding: const EdgeInsets.symmetric(vertical: 16),
                  enabledBorder: const UnderlineInputBorder(
                    borderSide: BorderSide(color: Color(0xFFD7D9D8)),
                  ),
                  focusedBorder: const UnderlineInputBorder(
                    borderSide: BorderSide(
                      color: Color(0xFF5CA887),
                      width: 1.5,
                    ),
                  ),
                  errorBorder: const UnderlineInputBorder(
                    borderSide: BorderSide(color: Color(0xFFC96363)),
                  ),
                  focusedErrorBorder: const UnderlineInputBorder(
                    borderSide: BorderSide(
                      color: Color(0xFFC96363),
                      width: 1.5,
                    ),
                  ),
                ),
                onChanged: (_) {
                  if (_errorMessage != null) {
                    setState(() {
                      _errorMessage = null;
                    });
                  }
                },
                onSubmitted: (_) => _submit(),
              ),

              const Spacer(),

              PrimaryButton(
                label: '완료',
                isLoading: _isLoading,
                onPressed: _isLoading ? null : _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
