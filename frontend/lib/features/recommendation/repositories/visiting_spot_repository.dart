import '../services/visiting_spot_api_service.dart';

class VisitingSpotRepository {
  VisitingSpotRepository({VisitingSpotApiService? api})
    : _api = api ?? VisitingSpotApiService();

  final VisitingSpotApiService _api;

  /// 방문 시작 → visitId 반환
  Future<int> startVisit({
    required int spotId,
    required double startLatitude,
    required double startLongitude,
  }) {
    return _api.startVisit(
      spotId: spotId,
      startLatitude: startLatitude,
      startLongitude: startLongitude,
    );
  }

  /// 방문 완료 처리
  Future<void> completeVisit({
    required int visitId,
    required double arrivedLatitude,
    required double arrivedLongitude,
    required int stayDurationSeconds,
  }) {
    return _api.completeVisit(
      visitId: visitId,
      arrivedLatitude: arrivedLatitude,
      arrivedLongitude: arrivedLongitude,
      stayDurationSeconds: stayDurationSeconds,
    );
  }
}
