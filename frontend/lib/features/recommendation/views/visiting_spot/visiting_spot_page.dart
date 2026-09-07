import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../../shared/widgets/app_button.dart';
import '../../models/recommendation.dart';
import '../../models/visit_status.dart';
import '../../services/naver_map_service.dart';
import '../../view_models/visiting_spot_view_model.dart';
import '../recommendation/widgets/recommendation_card.dart';
import 'widgets/visiting_spot_card.dart';
import 'widgets/visiting_status_card.dart';

class VisitingSpotPage extends StatefulWidget {
  final SpotDetail spot;

  const VisitingSpotPage({super.key, required this.spot});

  @override
  State<VisitingSpotPage> createState() => _VisitingSpotPageState();
}

class _VisitingSpotPageState extends State<VisitingSpotPage> {
  late final _viewModel = VisitingSpotViewModel(spot: widget.spot);

  // TODO: _viewModel.findAlternatives() 연동 전까지 화면 확인용 더미. 실제로는 대체지 API 응답으로 교체.
  static final List<Spot> _dummyAlternatives = [
    Spot(
      id: 101,
      name: '가덕도 해안공원',
      category: 'PARK',
      modes: const ['SCENERY'],
      latitude: 35.0294,
      longitude: 128.8103,
      quietScore: 88,
      quietLevel: 'QUIET',
      quietScoreUpdatedAt: DateTime.now(),
      address: '부산광역시 강서구 가덕해안로',
      imageUrl: 'https://placehold.co/400x300/png?text=Alt+1',
    ),
    Spot(
      id: 102,
      name: '몰운대',
      category: 'PARK',
      modes: const ['CONTEMPLATION'],
      latitude: 35.0511,
      longitude: 128.9645,
      quietScore: 81,
      quietLevel: 'QUIET',
      quietScoreUpdatedAt: DateTime.now(),
      address: '부산광역시 사하구 몰운대1길',
      imageUrl: 'https://placehold.co/400x300/png?text=Alt+2',
    ),
    Spot(
      id: 103,
      name: '다대포 해수욕장',
      category: 'BEACH',
      modes: const ['SCENERY'],
      latitude: 35.0489,
      longitude: 128.9651,
      quietScore: 76,
      quietLevel: 'QUIET',
      quietScoreUpdatedAt: DateTime.now(),
      address: '부산광역시 사하구 다대동',
      imageUrl: 'https://placehold.co/400x300/png?text=Alt+3',
    ),
  ];

  @override
  void dispose() {
    _viewModel.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final spot = widget.spot;

    return PopScope(
      canPop: false,
      child: Scaffold(
        backgroundColor: const Color(0xFFF7F9F8),
        appBar: AppBar(
          automaticallyImplyLeading: false,
          backgroundColor: const Color(0xFFF7F9F8),
          elevation: 0,
          centerTitle: true,
          title: const Text(
            '방문중인 장소',
            style: TextStyle(
              fontFamily: 'Paperlogy',
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: Colors.black,
            ),
          ),
        ),
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
            child: ListenableBuilder(
              listenable: _viewModel,
              builder: (context, _) {
                if (_viewModel.isStarting) {
                  return _buildStartingBody();
                }
                if (_viewModel.startError != null) {
                  return _buildStartErrorBody(_viewModel.startError!);
                }
                if (_viewModel.status == VisitStatus.crowdingDetected) {
                  return _buildAlternativesBody(spot);
                }
                return _buildVisitingBody(spot);
              },
            ),
          ),
        ),
      ),
    );
  }

  // 방문 시작 중 (visitId 발급 대기)
  Widget _buildStartingBody() {
    return const Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          CircularProgressIndicator(color: Color(0xFF589C7E)),
          SizedBox(height: 16),
          Text(
            '방문을 시작하는 중...',
            style: TextStyle(fontFamily: 'Paperlogy', fontSize: 14),
          ),
        ],
      ),
    );
  }

  // 방문 시작 실패 (위치 조회 실패 / 진행 중 방문 존재 등)
  Widget _buildStartErrorBody(String message) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          message,
          textAlign: TextAlign.center,
          style: const TextStyle(fontFamily: 'Paperlogy', fontSize: 14),
        ),
        const SizedBox(height: 20),
        PrimaryButton(
          label: '다시 시도',
          icon: Icons.refresh,
          onPressed: _viewModel.retryStartVisit,
        ),
        const SizedBox(height: 5),
        PrimaryButton(
          isOutlined: true,
          label: '돌아가기',
          onPressed: () => Get.back(),
        ),
      ],
    );
  }

  // 평상시 방문 화면 (visiting / notAtSpot / completed 공용)
  Widget _buildVisitingBody(SpotDetail spot) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            VisitingSpotCard(spot: spot),
            const SizedBox(height: 12),
            VisitingStatusCard(
              status: _viewModel.status,
              onReturnToVisiting: _viewModel.returnToVisiting,
            ),
          ],
        ),
        Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (_viewModel.errorMessage != null) ...[
              Text(
                _viewModel.errorMessage!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontFamily: 'Paperlogy',
                  fontSize: 12,
                  color: Color(0xFFC0392B),
                ),
              ),
              const SizedBox(height: 8),
            ],
            PrimaryButton(
              label: '방문 완료하기',
              icon: Icons.check,
              isLoading: _viewModel.isCompleting,
              onPressed: _viewModel.completeVisit,
            ),
            const SizedBox(height: 5),
            PrimaryButton(
              label: '네이버 지도에서 길찾기',
              icon: Icons.map,
              onPressed: () => openInNaverMap(
                lat: spot.latitude,
                lng: spot.longitude,
                name: spot.name,
              ),
            ),
            const SizedBox(height: 5),
            PrimaryButton(
              isOutlined: true,
              label: '방문 취소하기',
              icon: Icons.close,
              onPressed: () => Get.back(),
            ),
          ],
        ),
      ],
    );
  }

  // 고요지수 하락 감지 화면 — 피그마 "대체 장소 추천 화면" 대응
  Widget _buildAlternativesBody(SpotDetail spot) {
    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                VisitingSpotCard(spot: spot),
                const SizedBox(height: 12),
                VisitingStatusCard(
                  status: _viewModel.status,
                  onReturnToVisiting: _viewModel.returnToVisiting,
                ),
                const SizedBox(height: 24),
                const Text(
                  '다른 장소 둘러보기',
                  style: TextStyle(
                    fontFamily: 'Paperlogy',
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Colors.black,
                  ),
                ),
                const SizedBox(height: 12),
                // TODO: _viewModel.findAlternatives() 결과(List<AlternativeSpot>)로 교체
                for (final alt in _dummyAlternatives) ...[
                  RecommendationCard(spot: alt),
                  const SizedBox(height: 16),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        PrimaryButton(
          label: '기존 목적지 유지하기',
          onPressed: _viewModel.keepCurrentSpot,
        ),
      ],
    );
  }
}
