import 'dart:async';
import 'dart:developer' as developer;

import 'package:flutter/widgets.dart';
import 'package:geolocator/geolocator.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../../../core/network/api_exception.dart';
import '../models/alternative_spot.dart';
import '../models/current_visit.dart';
import '../models/recommendation.dart';
import '../models/visit_status.dart';
import '../repositories/visiting_spot_repository.dart';

class VisitingSpotViewModel extends ChangeNotifier with WidgetsBindingObserver {
  VisitingSpotViewModel({
    required this.spot,
    VisitingSpotRepository? repository,
  }) : _repository = repository ?? VisitingSpotRepository(),
       _startQuietScore = spot.quietScore,
       _currentQuietScore = spot.quietScore {
    _startVisit();
  }

  VisitingSpotViewModel.resume({
    required this.spot,
    required int resumedVisitId,
    required int? startQuietScore,
    required int? currentQuietScore,
    VisitingSpotRepository? repository,
  }) : _repository = repository ?? VisitingSpotRepository(),
       // ignore: prefer_initializing_formals
       _startQuietScore = startQuietScore,
       // ignore: prefer_initializing_formals
       _currentQuietScore = currentQuietScore {
    visitId = resumedVisitId;
    isStarting = false;
    WidgetsBinding.instance.addObserver(this);
    _scheduleRefresh();
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

  // 방문 완료
  bool isCompleting = false;
  String? errorMessage;

  // 방문 취소
  bool isCancelling = false;

  // 대체지 추천
  List<AlternativeSpot> alternatives = [];
  bool isLoadingAlternatives = false;
  String? alternativesError; // 빈 배열이면 "대체지 없음" 안내

  Timer? _refreshTimer;
  Future<CurrentVisit?>? _currentVisitRequest;
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

    for (final h in _refreshHours) {
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
      debugPrint('[VisitingSpotViewModel] 방문 시작 요청: spotId=${spot.id}');
      visitId = await _repository.startVisit(spotId: spot.id);
      debugPrint('[VisitingSpotViewModel] 방문 시작 성공: visitId=$visitId');
      if (_disposed) return; // 시작 요청 중 화면이 닫혔으면 옵저버/타이머 안 검
      WidgetsBinding.instance.addObserver(this);
      _scheduleRefresh();
    } on ApiException catch (e) {
      debugPrint(
        '[VisitingSpotViewModel] 방문 시작 API 오류: '
        'code=${e.code}, status=${e.statusCode}, message=${e.message}',
      );
      switch (e.code) {
        case 'AlreadyOngoingVisitException': // 409
          await _recoverOngoingVisit();
        case 'SpotNotFoundException': // 404
          startError = '장소를 찾을 수 없어요.';
        default:
          startError = e.message;
      }
    } catch (error, stackTrace) {
      debugPrint('[VisitingSpotViewModel] 방문 시작 원본 오류: $error\n$stackTrace');
      startError = '방문을 시작하지 못했어요. 다시 시도해주세요.';
    } finally {
      isStarting = false;
      _safeNotify();
    }
  }

  Future<void> _recoverOngoingVisit() async {
    try {
      final current = await _repository.getCurrentVisit();
      if (_disposed) return;

      if (current == null || current.status != 'STARTED') {
        startError = '이미 진행 중인 방문이 있어요.';
        return;
      }

      debugPrint(
        '[VisitingSpotViewModel] 기존 방문 발견: '
        'visitId=${current.visitId}, spotId=${current.spotId}',
      );

      Get.offNamed(
        AppRoutes.visitingSpot,
        arguments: {
          'spot': current.toPlaceholderSpotDetail(),
          'visit': current,
        },
      );
    } catch (error, stackTrace) {
      debugPrint('[VisitingSpotViewModel] 기존 방문 복구 실패: $error\n$stackTrace');
      startError = '이미 진행 중인 방문이 있어요.';
    }
  }

  Future<void> retryStartVisit() => _startVisit();

  Future<Position> _currentPosition() async {
    if (!await Geolocator.isLocationServiceEnabled()) {
      throw const _LocationUnavailableException('기기의 위치 서비스를 켠 뒤 다시 시도해주세요.');
    }

    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied) {
      throw const _LocationUnavailableException('방문을 확인하려면 위치 권한이 필요해요.');
    }
    if (permission == LocationPermission.deniedForever) {
      throw const _LocationUnavailableException('설정에서 위치 권한을 허용한 뒤 다시 시도해주세요.');
    }

    return Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
        timeLimit: Duration(seconds: 10),
      ),
    );
  }

  /// 메모리의 visitId가 유실되었으면 서버의 진행 중 방문으로 복구한다.
  Future<int?> _resolveActiveVisitId({required bool requireSameSpot}) async {
    if (visitId != null) return visitId;

    debugPrint('[VisitingSpotViewModel] visitId 없음, 현재 방문 조회로 복구 시도');
    final request = _currentVisitRequest ??= _repository.getCurrentVisit();
    final CurrentVisit? current;
    try {
      current = await request;
    } finally {
      if (identical(_currentVisitRequest, request)) {
        _currentVisitRequest = null;
      }
    }
    if (current == null) {
      errorMessage = '진행 중인 방문이 없어요. 장소 상세에서 다시 시작해주세요.';
      return null;
    }

    debugPrint(
      '[VisitingSpotViewModel] 현재 방문 조회 성공: '
      'visitId=${current.visitId}, spotId=${current.spotId}, '
      'selectedSpotId=${spot.id}',
    );
    if (requireSameSpot && current.spotId != spot.id) {
      errorMessage = '현재 선택한 장소와 진행 중인 방문 장소가 달라요. 기존 방문을 먼저 취소해주세요.';
      return null;
    }

    visitId = current.visitId;
    debugPrint('[VisitingSpotViewModel] 방문 세션 복구 성공: visitId=$visitId');
    return visitId;
  }

  Future<bool> cancelVisit() async {
    if (isCancelling) return false;

    isCancelling = true;
    errorMessage = null;
    _safeNotify();

    try {
      debugPrint('[VisitingSpotViewModel] 방문 취소 버튼 클릭: visitId=$visitId');
      final activeVisitId = await _resolveActiveVisitId(requireSameSpot: false);
      if (activeVisitId == null) return false;

      developer.log(
        '방문 취소 요청: visitId=$activeVisitId',
        name: 'VisitingSpotViewModel',
      );
      await _repository.cancelVisit(visitId: activeVisitId);
      if (_disposed) return false;

      _refreshTimer?.cancel();
      return true;
    } on ApiException catch (e) {
      switch (e.code) {
        case 'InvalidVisitStateException':
          errorMessage = '이미 종료된 방문이라 취소할 수 없어요.';
        case 'VisitNotFoundException':
          errorMessage = '방문 정보를 찾을 수 없어요.';
        default:
          errorMessage = e.message;
      }
    } catch (_) {
      errorMessage = '방문 취소에 실패했어요. 다시 시도해주세요.';
    } finally {
      isCancelling = false;
      _safeNotify();
    }
    return false;
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
    if (isCompleting) return;

    isCompleting = true;
    errorMessage = null;
    _safeNotify();

    try {
      debugPrint('[VisitingSpotViewModel] 방문 완료 버튼 클릭: visitId=$visitId');
      final activeVisitId = await _resolveActiveVisitId(requireSameSpot: true);
      if (activeVisitId == null) return;

      final radius = spot.visitRadiusMeters;
      if (radius == null) {
        errorMessage = '방문 가능 반경 정보를 확인할 수 없어요.';
        return;
      }

      developer.log(
        '방문 완료 위치 확인 시작: visitId=$activeVisitId, '
        'spotId=${spot.id}, radius=${radius}m',
        name: 'VisitingSpotViewModel',
      );
      final pos = await _currentPosition();
      final distanceMeters = Geolocator.distanceBetween(
        pos.latitude,
        pos.longitude,
        spot.latitude,
        spot.longitude,
      );
      developer.log(
        '방문 거리 계산: current=(${pos.latitude}, ${pos.longitude}), '
        'spot=(${spot.latitude}, ${spot.longitude}), '
        'distance=${distanceMeters.toStringAsFixed(1)}m',
        name: 'VisitingSpotViewModel',
      );

      if (distanceMeters > radius) {
        status = VisitStatus.notAtSpot;
        return;
      }

      developer.log(
        '방문 반경 진입 확인, 완료 API 호출: visitId=$activeVisitId',
        name: 'VisitingSpotViewModel',
      );
      await _repository.completeVisit(visitId: activeVisitId);
      if (_disposed) return; // 완료 요청 중 화면이 닫혔으면 이동하지 않음

      _refreshTimer?.cancel();
      status = VisitStatus.completed;
      notifyListeners();

      Get.offNamed(
        AppRoutes.reviewWrite,
        arguments: {'spot': spot, 'visitId': activeVisitId},
      );
    } on _LocationUnavailableException catch (e) {
      errorMessage = e.message;
    } on ApiException catch (e) {
      switch (e.code) {
        case 'InvalidVisitStateException': // 409 — 이미 완료/취소된 방문
          errorMessage = '이미 완료되었거나 취소된 방문이에요.';
        case 'VisitNotFoundException': // 404
          errorMessage = '방문 정보를 찾을 수 없어요.';
        default:
          errorMessage = e.message;
      }
    } catch (error, stackTrace) {
      developer.log(
        '방문 완료 처리 실패',
        name: 'VisitingSpotViewModel',
        error: error,
        stackTrace: stackTrace,
      );
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

class _LocationUnavailableException implements Exception {
  const _LocationUnavailableException(this.message);

  final String message;
}
