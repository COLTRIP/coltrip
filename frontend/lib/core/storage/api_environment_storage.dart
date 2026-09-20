import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 운영/시연 API 환경 선택 상태를 기기에 저장합니다.
class ApiEnvironmentStorage {
  const ApiEnvironmentStorage();

  static const _storage = FlutterSecureStorage();
  static const _demoModeKey = 'demoModeEnabled';

  Future<bool> isDemoMode() async {
    return await _storage.read(key: _demoModeKey) == 'true';
  }

  Future<void> setDemoMode(bool enabled) {
    return _storage.write(key: _demoModeKey, value: enabled.toString());
  }
}
