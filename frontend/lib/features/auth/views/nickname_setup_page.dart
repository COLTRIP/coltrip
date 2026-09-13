import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/primary_button.dart';
import '../data/terms_content.dart';
import 'terms_detail_page.dart';

class NicknameSetupPage extends StatefulWidget {
  const NicknameSetupPage({super.key});

  @override
  State<NicknameSetupPage> createState() => _NicknameSetupPageState();
}

class _NicknameSetupPageState extends State<NicknameSetupPage> {
  bool _isAllChecked = false;
  bool _isServiceChecked = false;
  bool _isPrivacyChecked = false;
  bool _isLocationChecked = false;

  bool get _canProceed =>
      _isServiceChecked && _isPrivacyChecked && _isLocationChecked;

  final TextEditingController _nicknameController = TextEditingController();

  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  final Dio _dio = DioClient.instance;

  void _onAllChecked(bool? value) {
    if (value == null) return;
    setState(() {
      _isAllChecked = value;
      _isServiceChecked = value;
      _isPrivacyChecked = value;
      _isLocationChecked = value;
    });
  }

  void _openTerms(String title, String content) {
    Get.to(() => TermsDetailPage(title: title, content: content));
  }

  void _onItemChecked() {
    setState(() {
      _isAllChecked =
          _isServiceChecked && _isPrivacyChecked && _isLocationChecked;
    });
  }

  Future<void> _submitNickname() async {
    final nickname = _nicknameController.text.trim();

    debugPrint('입력한 닉네임: $nickname');

    final accessToken = await _storage.read(key: 'access_token');

    debugPrint('토큰 존재: ${accessToken != null}');

    final response = await _dio.patch(
      '/api/users/me',
      data: {'nickname': nickname},
      options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
    );
    debugPrint('닉네임 저장 상태: ${response.statusCode}');
    debugPrint('닉네임 저장 응답: ${response.data}');

    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('has_agreed_terms', true);

    if (!mounted) return;

    Get.offAllNamed(AppRoutes.main);
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
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 40),

              Center(child: Image.asset('assets/images/logo.png')),

              const SizedBox(height: 40),

              const Text(
                '닉네임(2-15자리 한글 및 영문)',
                style: TextStyle(
                  fontFamily: 'Paperlogy',
                  fontWeight: FontWeight.w500,
                  color: Color(0xFF252B28),
                ),
              ),

              TextField(
                controller: _nicknameController,
                style: const TextStyle(
                  fontFamily: 'Paperlogy',
                  fontWeight: FontWeight.w400,
                ),
                decoration: const InputDecoration(
                  focusedBorder: UnderlineInputBorder(
                    borderSide: BorderSide(color: Color(0xFF252B28)),
                  ),
                ),
              ),

              const SizedBox(height: 30),

              const Text(
                '앱 이용을 위해 약관 동의가 필요합니다.',
                style: TextStyle(
                  fontFamily: 'Paperlogy',
                  fontWeight: FontWeight.w500,
                  color: Color(0xFF252B28),
                ),
              ),
              const SizedBox(height: 10),
              CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                activeColor: const Color(0xFF589C7E),
                title: const Text(
                  '전체 동의하기',
                  style: TextStyle(
                    fontFamily: 'Paperlogy',
                    fontWeight: FontWeight.w500,
                    color: Color(0xFF252B28),
                  ),
                ),
                value: _isAllChecked,
                onChanged: _onAllChecked,
                controlAffinity: ListTileControlAffinity.leading,
              ),

              _buildTermsRow(
                title: '[필수] 서비스 이용약관 동의',
                value: _isServiceChecked,
                onChanged: (val) {
                  _isServiceChecked = val ?? false;
                  _onItemChecked();
                },
                onViewDetail: () =>
                    _openTerms('서비스 이용약관', TermsContent.serviceTerms),
              ),

              _buildTermsRow(
                title: '[필수] 개인정보 처리방침 동의',
                value: _isPrivacyChecked,
                onChanged: (val) {
                  _isPrivacyChecked = val ?? false;
                  _onItemChecked();
                },
                onViewDetail: () =>
                    _openTerms('개인정보 처리방침', TermsContent.privacyPolicy),
              ),

              _buildTermsRow(
                title: '[필수] 위치기반서비스 이용약관 동의',
                value: _isLocationChecked,
                onChanged: (val) {
                  _isLocationChecked = val ?? false;
                  _onItemChecked();
                },
                onViewDetail: () =>
                    _openTerms('위치기반서비스 이용약관', TermsContent.locationTerms),
              ),
              const SizedBox(height: 30),

              PrimaryButton(
                label: '시작하기',
                onPressed: _canProceed ? _submitNickname : null,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTermsRow({
    required String title,
    required bool value,
    required ValueChanged<bool?> onChanged,
    required VoidCallback onViewDetail,
  }) {
    return Row(
      children: [
        Expanded(
          child: CheckboxListTile(
            title: Text(
              title,
              style: const TextStyle(
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w500,
                color: Color(0xFF252B28),
                fontSize: 14,
              ),
            ),
            activeColor: const Color(0xFF589C7E),
            value: value,
            onChanged: onChanged,
            controlAffinity: ListTileControlAffinity.leading,
            contentPadding: EdgeInsets.zero,
          ),
        ),
        IconButton(
          icon: const Icon(
            Icons.arrow_forward_ios,
            size: 16,
            color: Colors.grey,
          ),
          onPressed: onViewDetail,
        ),
      ],
    );
  }
}
