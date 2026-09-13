import 'package:flutter/foundation.dart';
import 'package:geolocator/geolocator.dart';

class LocationPermissionViewModel extends ChangeNotifier {
  String? errorMessage;
  bool isPermanentlyDenied = false;
  bool granted = false;

  bool _requested = false; // OS 권한 팝업(또는 설정 화면)을 한 번이라도 띄웠는지

  // OS 팝업 없이 현재 위치 권한 보유 여부만 확인 (방문 시작 시 권한 화면 스킵 판단용)
  static Future<bool> isGranted() async {
    if (!await Geolocator.isLocationServiceEnabled()) return false;
    final permission = await Geolocator.checkPermission();
    return permission == LocationPermission.whileInUse ||
        permission == LocationPermission.always;
  }

  Future<void> requestPermission() async {
    _requested = true;

    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      errorMessage = '기기의 위치 서비스가 꺼져있어요. 위치 서비스를 켜주세요.';
      notifyListeners();
      return;
    }

    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }

    if (permission == LocationPermission.deniedForever) {
      isPermanentlyDenied = true;
      errorMessage = '위치 권한이 거부됐어요. 설정에서 허용해주세요.';
      notifyListeners();
      return;
    }

    if (permission == LocationPermission.denied) {
      errorMessage = '위치 권한이 필요해요. 다시 시도해주세요.';
      notifyListeners();
      return;
    }


    granted = true;
    notifyListeners();
  }

  // 앱이 다시 포그라운드로 돌아왔을 때(OS 권한 팝업/설정 화면 복귀) 자동으로 재확인
  Future<void> checkPermissionOnResume() async {
    if (!_requested || granted) return;

    final permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.whileInUse || permission == LocationPermission.always) {
      granted = true;
      notifyListeners();
    }
  }

  Future<void> openAppSettings() => Geolocator.openAppSettings();
}
