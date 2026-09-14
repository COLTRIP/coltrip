import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/network/api_exception.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../profile/services/profile_service.dart';

class NicknameSetupPage extends StatefulWidget {
  const NicknameSetupPage({super.key});

  @override
  State<NicknameSetupPage> createState() => _NicknameSetupPageState();
}

class _NicknameSetupPageState extends State<NicknameSetupPage> {
  final TextEditingController _nicknameController = TextEditingController();
  final ProfileService _profileService = ProfileService();

  bool _isLoading = false;
  String? _errorMessage;

  Future<void> _submitNickname() async {
    if (_isLoading) return;

    FocusScope.of(context).unfocus();

    final nickname = _nicknameController.text.trim();

    if (nickname.isEmpty) {
      setState(() {
        _errorMessage = '닉네임을 입력해주세요.';
      });
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await _profileService.updateNickname(nickname: nickname);

      if (!mounted) return;

      Get.offAllNamed(AppRoutes.main);
    } on ApiException catch (error) {
      if (!mounted) return;

      setState(() {
        _errorMessage = error.message;
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Center(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Spacer(flex: 2),

                Center(child: Image.asset('assets/images/logo.png')),

                const SizedBox(height: 100),

                const Text(
                  '닉네임(2-15자리 한글 및 영문)',
                  style: TextStyle(
                    fontWeight: FontWeight.w500,
                    color: Color(0xFF252B28),
                  ),
                ),

                TextField(
                  controller: _nicknameController,
                  style: const TextStyle(fontWeight: FontWeight.w400),
                  textInputAction: TextInputAction.done,
                  decoration: InputDecoration(
                    errorText: _errorMessage,
                    focusedBorder: const UnderlineInputBorder(
                      borderSide: BorderSide(color: Color(0xFF252B28)),
                    ),
                  ),
                  onChanged: (_) {
                    if (_errorMessage != null) {
                      setState(() {
                        _errorMessage = null;
                      });
                    }
                  },
                  onSubmitted: (_) => _submitNickname(),
                ),

                const Spacer(flex: 10),

                PrimaryButton(
                  label: '시작하기',
                  isLoading: _isLoading,
                  onPressed: _isLoading ? null : _submitNickname,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
