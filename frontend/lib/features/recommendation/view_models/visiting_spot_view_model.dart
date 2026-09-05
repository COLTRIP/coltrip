import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:get/get.dart';

import '../models/recommendation.dart';
import '../models/visit_status.dart';
import '../views/review_writing/write_review_page.dart';

class VisitingSpotViewModel extends ChangeNotifier {
  VisitingSpotViewModel({required this.spot})
    : _startQuietScore = spot.quietScore,
      _currentQuietScore = spot.quietScore {
    _startPolling();
  }

  final SpotDetail spot;

  static const Duration _pollInterval = Duration(minutes: 5);
  static const int _dropThreshold = 15;
  static const int _absoluteThreshold = 40;

  VisitStatus status = VisitStatus.crowdingDetected;
  final int _startQuietScore;
  int _currentQuietScore;
  String? errorMessage;

  Timer? _pollTimer;

  int get startQuietScore => _startQuietScore;
  int get currentQuietScore => _currentQuietScore;

  void _startPolling() {
    _pollTimer = Timer.periodic(_pollInterval, (_) => _checkQuietScore());
  }

  Future<void> _checkQuietScore() async {
    // TODO: GET /api/spots/{spot.id} 로 현재 고요지수 재조회 후 _currentQuietScore 갱신
    // _currentQuietScore = await _repository.fetchQuietScore(spot.id);

    final dropped = _startQuietScore - _currentQuietScore >= _dropThreshold;
    final tooCrowded = _currentQuietScore < _absoluteThreshold;

    if (status == VisitStatus.visiting && (dropped || tooCrowded)) {
      status = VisitStatus.visiting;
    }
    notifyListeners();
  }

  Future<void> findAlternatives() async {
    // TODO: 위치 권한 확인/요청(LocationPermissionViewModel) 후
    //       GET /api/spots/{spot.id}/alternatives 호출 -> 결과 노출
  }

  Future<void> completeVisit() async {
    // TODO: 방문 완료 API 호출 후
    _pollTimer?.cancel();
    status = VisitStatus.notAtSpot;
    notifyListeners();
    Get.off(() => WriteReviewPage(spot: spot));
    //TODO: 엔포 연결시 수정
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    super.dispose();
  }
}
