import 'package:flutter/material.dart';
import 'package:flutter_naver_map/flutter_naver_map.dart';
import 'package:google_sign_in/google_sign_in.dart';

import 'app/app.dart';


Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await FlutterNaverMap().init(
    clientId: '2byxwtmr1o',
    onAuthFailed: (ex) {
      debugPrint('네이버 지도 인증 실패: $ex');
    },
  );
  await GoogleSignIn.instance.initialize(
    serverClientId:
        '888142954996-nr4dvp1qriea7ilnm5qidp9tc6qb9q40.apps.googleusercontent.com',
  );

  runApp(const ColtripApp());
}