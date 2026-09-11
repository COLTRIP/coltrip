import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:geolocator/geolocator.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/network/api_exception.dart';
import '../models/alternative_spot.dart';
import '../models/recommendation.dart';
import '../models/visit_status.dart';
import '../repositories/visiting_spot_repository.dart';

class VisitingSpotViewModel extends ChangeNotifier with WidgetsBindingObserver {
  VisitingSpotViewModel({
    required this.spot,
    VisitingSpotRepository? repository,
  })  : _repository = repository ?? VisitingSpotRepository(),
        _startQuietScore = spot.quietScore,
        _currentQuietScore = spot.quietScore {
    _startVisit();
  }

  final SpotDetail spot;
  final VisitingSpotRepository _repository;
  static const _refreshHours = [9, 13, 17, 21];

  static const int _dropThreshold = 15;
  static const int _absoluteThreshold = 40;

  VisitStatus status = VisitStatus.visiting;

  /// 방문 시작 시점의 고요지수 기준선. 시작 시 미계산(null)이면
  /// 첫 유효 재조회값으로 지연 설정한다. (null 로 두고 0 으로 굳히지 않음)
  int? _startQuietScore;
  int? _currentQuietScore;

  // 방문 세션
  int? visitId;
  bool isStarting = true;
  String? startError;
  DateTime? _visitStartedAt;

  // 방문 완료
  bool isCompleting = false;
  String? errorMessage;

  // 대체지 추천
  List<AlternativeSpot> alternatives = [];
  bool isLoadingAlternatives = false;
  String? alternativesError; // 빈 배열이면 "대체지 없음" 안내

  Timer? _refreshTimer;
  bool _disposed = false;

  int? get startQuietScore => _startQuietScore;
  int? get currentQuietScore => _currentQuietScore;

  /// dispose 이후 비동기 콜백이 늦게 도착해도 죽지 않도록
  void _safeNotify() {
    if (_disposed) return;
    notifyListeners();
  }

  DateTime _nextBoundary() {
    DateTime from = DateTime.now();

    for(final h in _refreshHours) {
      final c = DateTime(from.year, from.month, from.day, h);
      if (c.isAfter(from)) return c;
    }
    final t = from.add(const Duration(days: 1));
    return DateTime(t.year, t.month, t.day, _refreshHours.first);
  }



  Future<void> _startVisit() async {
    isStarting = true;
    startError = null;
    notifyListeners();

    try {
      final pos = await _currentPosition();
      visitId = await _repository.startVisit(
        spotId: spot.id,
        startLatitude: pos.latitude,
        startLongitude: pos.longitude,
      );
      if (_disposed) return; // 시작 요청 중 화면이 닫혔으면 옵저버/타이머 안 검
      _visitStartedAt = DateTime.now();
      WidgetsBinding.instance.addObserver(this);
      _scheduleRefresh();
    } on ApiException catch (e) {
      switch (e.code) {
        case 'AlreadyOngoingVisitException': // 409
          // TODO(진행 중 방문 복구): 그 방문 조회 후 방문 화면으로 이동
          startError = '이미 진행 중인 방문이 있어요.';
        case 'SpotNotFoundException': // 404
          startError = '장소를 찾을 수 없어요.';
        default:
          startError = e.message;
      }
    } catch (_) {
      startError = '방문을 시작하지 못했어요. 위치 확인 후 다시 시도해주세요.';
    } finally {
      isStarting = false;
      _safeNotify();
    }
  }

  Future<void> retryStartVisit() => _startVisit();

  Future<Position> _currentPosition() {
    return Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
        timeLimit: Duration(seconds: 10),
      ),
    );
  }


  /// 다음 리프레시 경계 시각에 맞춰 one-shot 타이머 예약
  void _scheduleRefresh() {
    _refreshTimer?.cancel();
    _refreshTimer = Timer(_nextBoundary().difference(DateTime.now()), () {
      _checkQuietScore();
      _scheduleRefresh(); // 방문이 계속되면 다음 경계로
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _checkQuietScore();
      _scheduleRefresh();
    }
  }

  Future<void> _checkQuietScore() async {
    final int? latest;
    try {
      latest = await _repository.viewQuietValue(spotId: spot.id);
    } catch (_) {
      return; // 재조회 실패 시 이번 주기는 건너뜀
    }
    if (_disposed) return; // 조회 도중 화면이 닫혔으면 중단
    if (latest == null) return; // 고요지수 미계산 스팟 → 판정 스킵

    _currentQuietScore = latest;
    // 방문 시작 때 고요지수가 없었으면 첫 유효 조회값을 기준선으로 삼는다
    final baseline = _startQuietScore ??= latest;

    final dropped = baseline - latest >= _dropThreshold;
    final tooCrowded = latest < _absoluteThreshold;

    if (status == VisitStatus.visiting && (dropped || tooCrowded)) {
      status = VisitStatus.crowdingDetected;
      findAlternatives(); // 대체지 목록 미리 로드
    }
    _safeNotify(); // 고요지수 표시 갱신 (+ 위에서 상태가 바뀌었으면 그것도 반영)
  }

  Future<void> findAlternatives() async {
    isLoadingAlternatives = true;
    alternativesError = null;
    _safeNotify();

    try {
      alternatives = await _repository.getAlternatives(spotId: spot.id);
      if (alternatives.isEmpty) {
        alternativesError = '지금은 추천할 만한 대체지가 없어요.';
      }
    } on ApiException catch (e) {
      alternativesError = e.message;
    } catch (_) {
      alternativesError = '대체지를 불러오지 못했어요.';
    } finally {
      isLoadingAlternatives = false;
      _safeNotify();
    }
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
    _safeNotify();

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
      if (_disposed) return; // 완료 요청 중 화면이 닫혔으면 이동하지 않음

      _refreshTimer?.cancel();
      status = VisitStatus.completed;
      notifyListeners();

      Get.offNamed(
        AppRoutes.reviewWrite,
        arguments: {'spot': spot, 'visitId': visitId!},
      );
    } on ApiException catch (e) {
      switch (e.code) {
        case 'VisitConditionNotMetException': // 400 — 목적지 반경 밖
          // 안내 박스 표시, 사용자가 아이콘 버튼 눌러야 visiting 으로 복귀
          status = VisitStatus.notAtSpot;
        case 'InvalidVisitStateException': // 409 — 이미 완료/취소된 방문
          errorMessage = '이미 완료되었거나 취소된 방문이에요.';
        case 'VisitNotFoundException': // 404
          errorMessage = '방문 정보를 찾을 수 없어요.';
        default:
          errorMessage = e.message;
      }
    } catch (_) {
      errorMessage = '방문 완료 처리에 실패했어요. 다시 시도해주세요.';
    } finally {
      isCompleting = false;
      _safeNotify();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    _refreshTimer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }
}
