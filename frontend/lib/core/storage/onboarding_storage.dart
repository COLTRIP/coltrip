import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 온보딩을 완료했는지 기기에 저장합니다.
class OnboardingStorage {
  const OnboardingStorage();

  static const _storage = FlutterSecureStorage();
  static const _completedKey = 'onboardingCompleted';

  Future<bool> hasCompleted() async {
    return await _storage.read(key: _completedKey) == 'true';
  }

  Future<void> markCompleted() {
    return _storage.write(key: _completedKey, value: 'true');
  }
}
