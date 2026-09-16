/// 앱에서 사용하는 라우트 경로를 한곳에서 관리합니다.
///
/// 화면 이동 시 문자열 경로를 직접 작성하지 않고
/// [AppRoutes]의 상수를 사용합니다.
abstract final class AppRoutes {
  // 앱 시작 및 온보딩
  static const startup = '/startup';
  static const onboarding = '/onboarding';

  // 메인 화면
  static const main = '/main';

  // 인증 및 사용자 설정
  static const login = '/login';
  static const nickname = '/nickname';
  static const changeNickname = '/nickname/change';
  static const terms = '/terms'; //이용 약관
  static const info = '/info';

  // 추천
  static const moodSelection = '/recommendation/mood-selection';
  static const recommendationList = '/recommendation/list';
  static const recommendationDetail = '/recommendation/detail';
  static const visitingSpot = '/recommendation/visiting';
  static const reviewList = '/recommendation/reviews';
  static const reviewWrite = '/recommendation/review-write';
  static const locationPermission = '/recommendation/location-permission';
}
