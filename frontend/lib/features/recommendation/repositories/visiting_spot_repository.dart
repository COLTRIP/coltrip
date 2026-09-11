import '../models/alternative_spot.dart';
import '../models/current_visit.dart';
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

  Future<int?> viewQuietValue({
    required int spotId,
  }) {
    return _api.viewQuietValue(spotId: spotId);
  }
  Future<List<AlternativeSpot>> getAlternatives({required int spotId}) {
    return _api.getAlternatives(spotId: spotId);
  }

  /// 진행 중인 방문 취소
  Future<void> cancelVisit({required int visitId}) {
    return _api.cancelVisit(visitId: visitId);
  }

  Future<CurrentVisit?> getCurrentVisit() {
    return _api.getCurrentVisit();
  }

  /// 방문 완료 처리
  Future<void> completeVisit({
    required int visitId,
    required double arrivedLatitude,
    required double arrivedLongitude,
  }) {
    return _api.completeVisit(
      visitId: visitId,
      arrivedLatitude: arrivedLatitude,
      arrivedLongitude: arrivedLongitude,
    );
  }
}
