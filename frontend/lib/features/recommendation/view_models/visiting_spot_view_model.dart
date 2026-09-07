import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:geolocator/geolocator.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/network/api_exception.dart';
import '../models/recommendation.dart';
import '../models/visit_status.dart';
import '../repositories/visiting_spot_repository.dart';

class VisitingSpotViewModel extends ChangeNotifier {
  VisitingSpotViewModel({
    required this.spot,
    VisitingSpotRepository? repository,
  })  : _repository = repository ?? VisitingSpotRepository(),
        _startQuietScore = spot.quietScore ?? 0,
        _currentQuietScore = spot.quietScore ?? 0 {
    _startVisit();
  }

  final SpotDetail spot;
  final VisitingSpotRepository _repository;

  static const Duration _pollInterval = Duration(minutes: 5);
  static const int _dropThreshold = 15;
  static const int _absoluteThreshold = 40;

  VisitStatus status = VisitStatus.visiting;
  final int _startQuietScore;
  int _currentQuietScore;

  // 방문 세션
  int? visitId;
  bool isStarting = true;
  String? startError;
  DateTime? _visitStartedAt;

  // 방문 완료
  bool isCompleting = false;
  String? errorMessage;

  Timer? _pollTimer;

  int get startQuietScore => _startQuietScore;
  int get currentQuietScore => _currentQuietScore;

  Future<void> _startVisit() async {
    isStarting = true;
    startError = null;
    notifyListeners();

    try {
      // 위치 권한은 진입 전(RecommendationDetailPage._startVisit)에서 확보됨
      final pos = await _currentPosition();
      visitId = await _repository.startVisit(
        spotId: spot.id,
        startLatitude: pos.latitude,
        startLongitude: pos.longitude,
      );
      _visitStartedAt = DateTime.now();
      _startPolling();
    } on ApiException catch (e) {
      // 409 AlreadyOngoingVisitException 등
      startError = e.message;
    } catch (_) {
      startError = '방문을 시작하지 못했어요. 위치 확인 후 다시 시도해주세요.';
    } finally {
      isStarting = false;
      notifyListeners();
    }
  }

  Future<void> retryStartVisit() => _startVisit();

  Future<Position> _currentPosition() {
    return Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(accuracy: LocationAccuracy.high),
    );
  }

  void _startPolling() {
    _pollTimer?.cancel();
    _pollTimer = Timer.periodic(_pollInterval, (_) => _checkQuietScore());
  }

  Future<void> _checkQuietScore() async {
    // TODO: GET /api/spots/{spot.id} 로 현재 고요지수 재조회 후 _currentQuietScore 갱신
    final dropped = _startQuietScore - _currentQuietScore >= _dropThreshold;
    final tooCrowded = _currentQuietScore < _absoluteThreshold;

    if (status == VisitStatus.visiting && (dropped || tooCrowded)) {
      status = VisitStatus.crowdingDetected;
      notifyListeners();
    }
  }

  Future<void> findAlternatives() async {
    // TODO: 위치 권한 확인/요청(LocationPermissionViewModel) 후
    //       GET /api/spots/{spot.id}/alternatives 호출 -> 결과 노출
  }

  /// 대체지 추천을 무시하고 기존 목적지로 방문 유지
  void keepCurrentSpot() {
    status = VisitStatus.visiting;
    notifyListeners();
  }

  /// notAtSpot 안내를 닫고 방문 중 화면으로 복귀 (아이콘 버튼에서 호출)
  void returnToVisiting() {
    if (status != VisitStatus.notAtSpot) return;
    status = VisitStatus.visiting;
    notifyListeners();
  }

  Future<void> completeVisit() async {
    if (visitId == null || isCompleting) return;

    isCompleting = true;
    errorMessage = null;
    notifyListeners();

    try {
      final pos = await _currentPosition();

      // TODO: 체류시간 판정 방식(연속/누적, 반경 진입 시각 기준) 확정 후 교체.
      //       현재는 방문 시작~완료 벽시계 차이로 임시 계산.
      final stayed = _visitStartedAt == null
          ? 0
          : DateTime.now().difference(_visitStartedAt!).inSeconds;

      await _repository.completeVisit(
        visitId: visitId!,
        arrivedLatitude: pos.latitude,
        arrivedLongitude: pos.longitude,
        stayDurationSeconds: stayed,
      );

      _pollTimer?.cancel();
      status = VisitStatus.completed;
      notifyListeners();

      Get.offNamed(
        AppRoutes.reviewWrite,
        arguments: {'spot': spot, 'visitId': visitId!},
      );
    } on ApiException catch (e) {
      if (e.statusCode == 400) {
        // VisitConditionNotMetException — 목적지 반경 밖 / 체류시간 미충족.
        // 안내 박스를 띄우고, 사용자가 아이콘 버튼을 눌러야 visiting 으로 복귀.
        status = VisitStatus.notAtSpot;
      } else {
        // 409 InvalidVisitStateException(이미 완료/취소) 등
        errorMessage = e.message;
      }
    } catch (_) {
      errorMessage = '방문 완료 처리에 실패했어요. 다시 시도해주세요.';
    } finally {
      isCompleting = false;
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    super.dispose();
  }
}
