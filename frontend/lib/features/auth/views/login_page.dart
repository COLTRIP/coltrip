import 'dart:async';

import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/storage/api_environment_storage.dart';
import '../../../shared/widgets/primary_button.dart';
import '../models/auth_intent.dart';
import '../services/demo_auth_service.dart';
import '../services/google_auth_service.dart';
import '../widgets/google_auth_button.dart';

/// Google 계정을 통해 로그인 및 회원가입을 진행하는 화면입니다.
///
/// 인증 결과에 따라 닉네임 설정 또는 메인 화면으로 이동하며, 인증 과정의 로딩 및 오류 상태를 관리합니다.
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final GoogleAuthService _authService = GoogleAuthService();
  final DemoAuthService _demoAuthService = DemoAuthService();
  static const _environmentStorage = ApiEnvironmentStorage();

  bool _isLoading = false;
  bool _isDemoMode = false;
  int _logoTapCount = 0;
  Timer? _logoTapTimer;

  @override
  void initState() {
    super.initState();
    _loadEnvironment();
  }

  Future<void> _loadEnvironment() async {
    final isDemoMode = await _environmentStorage.isDemoMode();
    if (!mounted) return;
    setState(() => _isDemoMode = isDemoMode);
  }

  Future<void> _onLogoTapped() async {
    if (_isLoading) return;

    _logoTapTimer?.cancel();
    _logoTapCount++;
    if (_logoTapCount < 5) {
      _logoTapTimer = Timer(const Duration(seconds: 2), () {
        _logoTapCount = 0;
      });
      return;
    }

    _logoTapCount = 0;
    setState(() => _isLoading = true);

    try {
      if (_isDemoMode) {
        await _demoAuthService.exitDemoMode();
        if (!mounted) return;
        setState(() => _isDemoMode = false);
        _showEnvironmentMessage('일반 모드로 전환했어요.');
        return;
      }

      await _demoAuthService.enableDemoMode();
      if (!mounted) return;
      setState(() => _isDemoMode = true);
      _showEnvironmentMessage('관리자 모드로 전환했어요.');
    } catch (error, stackTrace) {
      debugPrint('관리자 모드 전환 실패: $error');
      debugPrintStack(stackTrace: stackTrace);
      _showError('관리자 모드로 전환하지 못했어요.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _startDemoSession() async {
    if (_isLoading) return;
    setState(() => _isLoading = true);

    try {
      await _demoAuthService.startGuestSession();
      if (!mounted) return;
      Get.offAllNamed(AppRoutes.main);
      Get.rawSnackbar(
        snackPosition: SnackPosition.BOTTOM,
        backgroundColor: const Color(0xFF589C7E),
        messageText: const Text(
          '관리자 모드로 시작했어요.',
          style: TextStyle(color: Colors.white),
        ),
      );
    } catch (error, stackTrace) {
      debugPrint('관리자 모드 게스트 로그인 실패: $error');
      debugPrintStack(stackTrace: stackTrace);
      _showError('관리자 모드에 연결하지 못했어요. 다시 시도해주세요.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showEnvironmentMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: const Color(0xFF589C7E),
      ),
    );
  }

  @override
  void dispose() {
    _logoTapTimer?.cancel();
    super.dispose();
  }

  Future<void> _authenticateWithGoogle({required AuthIntent intent}) async {
    if (_isLoading) return;

    setState(() {
      _isLoading = true;
    });

    try {
      final response = await _authService.authenticate(intent: intent);

      final nickname = response.user.nickname;
      final needsOnboarding = nickname == null || nickname.trim().isEmpty;

      debugPrint('인증 방식: ${intent.name}');
      debugPrint('신규 사용자: ${response.isNewUser}');
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

    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _signInWithGoogle() async {
    await _authenticateWithGoogle(intent: AuthIntent.login);
  }

  Future<void> _signUpWithGoogle() async {
    await _authenticateWithGoogle(intent: AuthIntent.signup);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            children: [
              const Spacer(flex: 2),

              GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: _onLogoTapped,
                child: Column(
                  children: [Image.asset('assets/images/logo.png')],
                ),
              ),

              const SizedBox(height: 200),

              if (_isDemoMode) ...[
                const Text(
                  '관리자 모드',
                  style: TextStyle(
                    color: Color(0xFF39765D),
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 12),
                PrimaryButton(
                  label: '관리자 모드로 시작하기',
                  isLoading: _isLoading,
                  onPressed: _startDemoSession,
                ),
              ] else ...[
                GoogleAuthButton(
                  label: 'Google로 로그인',
                  isLoading: _isLoading,
                  onPressed: _signInWithGoogle,
                ),

                const SizedBox(height: 20),

                const Row(
                  children: [
                    Expanded(child: Divider(color: Color(0xFFE0E0E0))),
                    Padding(
                      padding: EdgeInsets.symmetric(horizontal: 16),
                      child: Text(
                        '처음이신가요?',
                        style: TextStyle(
                          color: Color(0xFF6F7773),
                          fontWeight: FontWeight.w400,
                          fontSize: 15,
                        ),
                      ),
                    ),
                    Expanded(child: Divider(color: Color(0xFFE0E0E0))),
                  ],
                ),

                const SizedBox(height: 20),

                GoogleAuthButton(
                  label: 'Google로 회원가입',
                  isLoading: _isLoading,
                  onPressed: _signUpWithGoogle,
                ),
              ],

              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}
