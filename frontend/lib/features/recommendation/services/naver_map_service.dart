import 'dart:io' show Platform;

import 'package:url_launcher/url_launcher.dart';

Future<void> openInNaverMap({
  required double lat, // 목적지 위도
  required double lng, // 목적지 경도
  required String name, // 목적지 이름
}) async {
  final appName = Uri.encodeComponent('coltrip'); // "coltrip으로 돌아가기" 버튼 표시용
  final placeName = Uri.encodeComponent(name);

  // slat/slng를 안넣어서 현재 위치를 자동으로 출발 위치로
  final nmapUri = Uri.parse(
    'nmap://route/walk?dlat=$lat&dlng=$lng&dname=$placeName&appname=$appName',
  );

  try {
    if (await canLaunchUrl(nmapUri)) {
      final launched = await launchUrl(nmapUri);
      if (launched) return;
    }
  } catch (_) {
    // 일부 기기는 canLaunchUrl 자체가 예외를 던져서 무시하고 폴백 진행
  }

  await _openStoreFallback(); // 네이버지도 미설치 시 스토어로
}

Future<void> _openStoreFallback() async {
  final Uri storeUri;
  if (Platform.isIOS) {
    storeUri = Uri.parse('https://apps.apple.com/kr/app/id311867728');
  } else if (Platform.isAndroid) {
    storeUri = Uri.parse('https://play.google.com/store/apps/details?id=com.nhn.android.nmap');
  } else {
    throw Exception('지원하지 않는 플랫폼입니다.');
  }

  final launched = await launchUrl(storeUri, mode: LaunchMode.externalApplication);
  if (!launched) throw Exception('네이버지도 앱/스토어를 열 수 없습니다');
}
