import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/primary_button.dart';


class NicknameSetupPage extends StatefulWidget {
  const NicknameSetupPage({super.key});

  @override
  State<NicknameSetupPage> createState() => _NicknameSetupPageState();
}

class _NicknameSetupPageState extends State<NicknameSetupPage> {
  final TextEditingController _nicknameController = TextEditingController();

  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  final Dio _dio = DioClient.instance;

  Future<void> _submitNickname() async {
    final nickname = _nicknameController.text.trim();

    debugPrint('입력한 닉네임: $nickname');

    final accessToken = await _storage.read(
      key: 'access_token',
    );

    debugPrint('토큰 존재: ${accessToken != null}');

    final response = await _dio.patch(
      '/api/users/me',
      data: {
        'nickname': nickname,
      },
      options: Options(
        headers: {
          'Authorization': 'Bearer $accessToken',
        },
      ),
    );
    debugPrint('닉네임 저장 상태: ${response.statusCode}');
    debugPrint('닉네임 저장 응답: ${response.data}');

    Get.offAllNamed(AppRoutes.map);
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
          padding: const EdgeInsets.symmetric(
            horizontal: 24,
          ),
          child: Center(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Spacer(flex: 2),

                Center(
                  child: Image.asset('assets/images/logo.png')
                ),

                const SizedBox(height: 100),

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
                      borderSide: BorderSide(
                        color: Color(0xFF252B28),
                      ),
                    ),
                  ),
                ),

                const Spacer(flex: 10),

                PrimaryButton(
                  label: '시작하기',
                  onPressed: _submitNickname,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
