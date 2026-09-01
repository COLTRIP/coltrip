import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:get/get.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../../../app/routes/app_routes.dart';
import '../models/auth_intent.dart';
import '../services/google_auth_service.dart';
import '../widgets/google_auth_button.dart';


class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final GoogleAuthService _authService = GoogleAuthService();
  final FlutterSecureStorage _storage =
  const FlutterSecureStorage();

  bool _isLoading = false;

  Future<void> _authenticateWithGoogle({
    required AuthIntent intent,
  }) async {
    if (_isLoading) return;

    setState(() {
      _isLoading = true;
    });

    try {
      final response = await _authService.authenticate(
        intent: intent,
      );

      await _storage.write(
        key: 'access_token',
        value: response.accessToken,
      );

      await _storage.write(
        key: 'refresh_token',
        value: response.refreshToken,
      );

      final nickname = response.user.nickname;
      final needsOnboarding =
          nickname == null || nickname.trim().isEmpty;

      debugPrint('인증 방식: ${intent.name}');
      debugPrint('인증 사용자: ${response.user.email}');
      debugPrint('신규 사용자: ${response.isNewUser}');
      debugPrint('닉네임: $nickname');
      debugPrint('추가정보 입력 필요: $needsOnboarding');

      if (intent == AuthIntent.signup) {
        // 신규 회원가입이거나 가입 도중 닉네임을 설정하지 않은 사용자
        if (response.isNewUser || needsOnboarding) {
          Get.offAllNamed(AppRoutes.nickname);
          return;
        }

        _showError('이미 가입된 계정이에요. 로그인을 이용해주세요.');
        return;
      }

      // 기존 사용자의 로그인
      if (needsOnboarding) {
        Get.offAllNamed(AppRoutes.nickname);
      } else {
        Get.offAllNamed(AppRoutes.main);
      }
    } on GoogleSignInException catch (error, stackTrace) {
      debugPrint('GoogleSignInException code: ${error.code}');
      debugPrint(
        'GoogleSignInException description: ${error.description}',
      );
      debugPrintStack(stackTrace: stackTrace);

      if (error.code == GoogleSignInExceptionCode.canceled) {
        return;
      }

      _showError('Google 인증에 실패했어요.');
    } on AuthException catch (error, stackTrace) {
      debugPrint('AuthException: ${error.message}');
      debugPrintStack(stackTrace: stackTrace);

      _showError(error.message);
    } catch (error, stackTrace) {
      debugPrint('Google 인증 오류: $error');
      debugPrintStack(stackTrace: stackTrace);

      _showError('인증 중 오류가 발생했어요.');
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _showError(String message) {
    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
      ),
    );
  }

  Future<void> _signInWithGoogle() async {
    await _authenticateWithGoogle(
      intent: AuthIntent.login,
    );
  }

  Future<void> _signUpWithGoogle() async {
    await _authenticateWithGoogle(
      intent: AuthIntent.signup,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: 24,
          ),
          child: Column(
            children: [
              const Spacer(flex: 2),

              Image.asset('assets/images/logo.png'),

              const SizedBox(height: 200),

              GoogleAuthButton(
                label: 'Google로 로그인',
                isLoading: _isLoading,
                onPressed: _signInWithGoogle,
              ),

              const SizedBox(height: 20),

              const Row(
                children: [
                  Expanded(
                    child: Divider(
                      color: Color(0xFFE0E0E0),
                    ),
                  ),
                  Padding(
                    padding: EdgeInsets.symmetric(
                      horizontal: 16,
                    ),
                    child: Text(
                      '처음이신가요?',
                      style: TextStyle(
                        color: Color(0xFF6F7773),
                        fontFamily: 'Paperlogy',
                        fontWeight: FontWeight.w400,
                        fontSize: 15,
                      ),
                    ),
                  ),
                  Expanded(
                    child: Divider(
                      color: Color(0xFFE0E0E0),
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 20),

              GoogleAuthButton(
                label: 'Google로 회원가입',
                isLoading: _isLoading,
                onPressed: _signUpWithGoogle,
              ),

              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}
