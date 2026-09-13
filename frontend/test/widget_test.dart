import 'package:flutter_test/flutter_test.dart';

import 'package:coltrip/app/routes/app_pages.dart';
import 'package:coltrip/app/routes/app_routes.dart';

void main() {
  test('AppPages: 라우트 이름 중복 없음', () {
    final names = AppPages.pages.map((p) => p.name).toList();
    expect(names.toSet().length, names.length);
  });

  test('AppPages: 추천 플로우 라우트가 모두 등록되어 있음', () {
    final names = AppPages.pages.map((p) => p.name);
    expect(
      names,
      containsAll(<String>[
        AppRoutes.recommendation,
        AppRoutes.recommendationDetail,
        AppRoutes.visitingSpot,
        AppRoutes.reviewList,
        AppRoutes.reviewWrite,
        AppRoutes.locationPermission,
      ]),
    );
  });
}
