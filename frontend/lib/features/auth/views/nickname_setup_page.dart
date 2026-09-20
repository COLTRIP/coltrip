import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/network/api_exception.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../profile/services/profile_service.dart';

/// 회원가입 과정에서 사용할 닉네임을 설정하는 화면입니다.
///
/// 입력한 닉네임을 서버에 저장하고, 설정이 완료되면 메인 화면으로 이동합니다.
class NicknameSetupPage extends StatefulWidget {
  const NicknameSetupPage({super.key});

  @override
  State<NicknameSetupPage> createState() => _NicknameSetupPageState();
}

class _NicknameSetupPageState extends State<NicknameSetupPage> {
  final TextEditingController _nicknameController = TextEditingController();
  final ProfileService _profileService = ProfileService();

  bool _isLoading = false;
  bool _isServiceTermsChecked = false;
  bool _isPrivacyPolicyChecked = false;
  bool _isLocationTermsChecked = false;
  String? _errorMessage;

  bool get _isAllTermsChecked =>
      _isServiceTermsChecked &&
      _isPrivacyPolicyChecked &&
      _isLocationTermsChecked;

  void _setAllTerms(bool? value) {
    final isChecked = value ?? false;

    setState(() {
      _isServiceTermsChecked = isChecked;
      _isPrivacyPolicyChecked = isChecked;
      _isLocationTermsChecked = isChecked;
    });
  }

  Future<void> _submitNickname() async {
    if (_isLoading) return;

    FocusScope.of(context).unfocus();

    if (!_isAllTermsChecked) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('필수 약관에 모두 동의해주세요.')));
      return;
    }

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
      resizeToAvoidBottomInset: true,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: SingleChildScrollView(
                  keyboardDismissBehavior:
                      ScrollViewKeyboardDismissBehavior.onDrag,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 56),

                      Center(child: Image.asset('assets/images/logo.png')),

                      const SizedBox(height: 72),

                      const Text(
                        '닉네임(1~20자리 한글, 영문 및 숫자)',
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

                      const SizedBox(height: 30),

                      Row(
                        children: [
                          const Expanded(
                            child: Text(
                              '앱 이용을 위해 약관 동의가 필요합니다.',
                              style: TextStyle(
                                fontWeight: FontWeight.w500,
                                color: Color(0xFF252B28),
                              ),
                            ),
                          ),
                          TextButton(
                            onPressed: () => Get.toNamed(AppRoutes.terms),
                            style: TextButton.styleFrom(
                              padding: EdgeInsets.zero,
                              minimumSize: Size.zero,
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                            ),
                            child: const Text(
                              '약관 보기 >',
                              style: TextStyle(
                                fontSize: 12,
                                color: Color(0xFF7C8581),
                              ),
                            ),
                          ),
                        ],
                      ),

                      const SizedBox(height: 8),

                      CheckboxListTile(
                        contentPadding: EdgeInsets.zero,
                        controlAffinity: ListTileControlAffinity.leading,
                        activeColor: const Color(0xFF589C7E),
                        title: const Text(
                          '전체 동의하기',
                          style: TextStyle(
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF252B28),
                          ),
                        ),
                        value: _isAllTermsChecked,
                        onChanged: _isLoading ? null : _setAllTerms,
                      ),

                      const Divider(height: 1, color: Color(0xFFE1E3E2)),

                      const SizedBox(height: 5),

                      _TermsCheckbox(
                        label: '[필수] 서비스 이용약관 동의',
                        value: _isServiceTermsChecked,
                        onChanged: _isLoading
                            ? null
                            : (value) {
                                setState(() {
                                  _isServiceTermsChecked = value ?? false;
                                });
                              },
                      ),
                      _TermsCheckbox(
                        label: '[필수] 개인정보 처리방침 동의',
                        value: _isPrivacyPolicyChecked,
                        onChanged: _isLoading
                            ? null
                            : (value) {
                                setState(() {
                                  _isPrivacyPolicyChecked = value ?? false;
                                });
                              },
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),

              PrimaryButton(
                label: '시작하기',
                isLoading: _isLoading,
                onPressed: _isLoading || !_isAllTermsChecked
                    ? null
                    : _submitNickname,
              ),

              const SizedBox(height: 36),
            ],
          ),
        ),
      ),
    );
  }
}

class _TermsCheckbox extends StatelessWidget {
  const _TermsCheckbox({
    required this.label,
    required this.value,
    required this.onChanged,
  });

  final String label;
  final bool value;
  final ValueChanged<bool?>? onChanged;

  @override
  Widget build(BuildContext context) {
    return CheckboxListTile(
      contentPadding: EdgeInsets.zero,
      controlAffinity: ListTileControlAffinity.leading,
      activeColor: const Color(0xFF589C7E),
      dense: true,
      title: Text(
        label,
        style: const TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w500,
          color: Color(0xFF252B28),
        ),
      ),
      value: value,
      onChanged: onChanged,
    );
  }
}
