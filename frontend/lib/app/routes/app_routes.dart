abstract final class AppRoutes {
  static const map = '/map';
  static const main = '/main';
  static const profile = '/profile';

  static const login = '/login';
  static const nickname = '/nickname';

  // 추천 진입: 장소유형 선택 → 감성 선택 → 추천 목록
  static const recommendation = '/recommendation';
  static const emotionSelection = '/recommendation/emotion-selection';
  static const recommendationList = '/recommendation/list';

  // 추천 플로우 하위 화면
  static const recommendationDetail = '/recommendation/detail';
  static const visitingSpot = '/recommendation/visiting';
  static const reviewList = '/recommendation/reviews';
  static const reviewWrite = '/recommendation/review-write';
  static const locationPermission = '/recommendation/location-permission';
}
