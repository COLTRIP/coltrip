# geolocator: ^13.0.2 — 위치 권한 & GPS 조회

## 1. pubspec.yaml에 geolocator 추가

```yaml
dependencies:
  geolocator: ^13.0.2
```

## 2. 플랫폼별 필수 설정 (android/ios), 안하면 권한 팝업이 안 뜨거나 위치 조회 실패!

```xml
<!-- Android — android/app/src/main/AndroidManifest.xml, <manifest> 바로 아래에 추가 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- iOS — ios/Runner/Info.plist, <dict> 안에 키 추가 (이 문구 없으면 팝업 자체가 안 뜸) -->
<key>NSLocationWhenInUseUsageDescription</key>
<string>장소 방문을 시작하면 현재 위치를 확인해 실제로 장소에 방문했는지 확인해요.</string>
```

## 3. 코드

```dart
import 'package:flutter/foundation.dart';

import 'package:geolocator/geolocator.dart';

class LocationPermissionViewModel extends ChangeNotifier {
  String? errorMessage;
  bool isPermanentlyDenied = false;
  bool granted = false;

  bool _requested = false; // OS 권한 팝업(또는 설정 화면)을 한 번이라도 띄웠는지

  Future<void> requestPermission() async {
    _requested = true;

    // 기기 위치 서비스(GPS) 자체가 꺼져 있는지 먼저 확인
    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      errorMessage = '기기의 위치 서비스가 꺼져있어요. 위치 서비스를 켜주세요.';
      notifyListeners();
      return;
    }

    // 현재 권한 상태 확인 → denied면 OS 팝업 요청
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }

    // 영구 거부 → requestPermission 다시 불러도 팝업 안 뜸, 설정 화면으로 유도해야 함
    if (permission == LocationPermission.deniedForever) {
      isPermanentlyDenied = true;
      errorMessage = '위치 권한이 거부됐어요. 설정에서 허용해주세요.';
      notifyListeners();
      return;
    }

    // 여전히 거부 → 재시도 유도
    if (permission == LocationPermission.denied) {
      errorMessage = '위치 권한이 필요해요. 다시 시도해주세요.';
      notifyListeners();
      return;
    }

    // whileInUse / always → 통과
    granted = true;
    notifyListeners();
  }

  // 앱이 다시 포그라운드로 돌아왔을 때(설정에서 허용 후 복귀) 자동으로 재확인
  Future<void> checkPermissionOnResume() async {
    if (!_requested || granted) return;

    final permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.whileInUse ||
        permission == LocationPermission.always) {
      granted = true;
      notifyListeners();
    }
  }

  // OS 앱 설정 화면 열기 (영구 거부 대응)
  Future<void> openAppSettings() => Geolocator.openAppSettings();
}
```

## 참고

### 주요 API

| 목적 | 메서드 |
| --- | --- |
| 위치 서비스(GPS) 켜짐 여부 | `Geolocator.isLocationServiceEnabled()` |
| 현재 권한 상태 확인 | `Geolocator.checkPermission()` |
| 권한 요청 (OS 팝업) | `Geolocator.requestPermission()` |
| 앱 설정 화면 열기 | `Geolocator.openAppSettings()` |
| 현재 위치 좌표 | `Geolocator.getCurrentPosition()` *(대체지 찾기 시점에 사용 예정)* |
| 위치 실시간 스트림 | `Geolocator.getPositionStream()` *(미사용 — 실시간 추적 안 함)* |
| 두 좌표 간 거리 계산 | `Geolocator.distanceBetween(...)` *(미사용)* |

### LocationPermission enum

| 값 | 의미 |
| --- | --- |
| `denied` | 거부됨 (재요청 가능) |
| `deniedForever` | 영구 거부 → OS 설정에서만 변경 |
| `whileInUse` | 앱 사용 중 허용 |
| `always` | 항상 허용 |

### 주의사항

- iOS는 `Info.plist` 목적 문구가 없으면 권한 팝업 자체가 안 뜬다
- `deniedForever` 상태에서는 `requestPermission()`을 다시 불러도 팝업이 안 뜸 → 반드시 설정 화면으로 유도
- 현재는 **권한 획득까지만** 구현. 실제 좌표 조회(`getCurrentPosition`)는 방문 중 "대체지 찾기" 시점에 추가 예정 ([visiting-flow.md](./visiting-flow.md) 참고). 실시간 위치 추적은 안 함
- 웹/데스크톱은 브라우저·OS 권한 모델이 달라 별도 확인 필요 (COLTRIP은 모바일 전용)
