import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';

import '../../../../shared/widgets/app_button.dart';
import '../../models/recommendation.dart';
import '../../view_models/location_permission_view_model.dart';
import '../../view_models/recommendation_detail_view_model.dart';
import '../location_permission/location_permission_page.dart';
import '../review/review_list_page.dart';
import '../review/widgets/review_section.dart';
import '../visiting_spot/visiting_spot_page.dart';
import 'widgets/quiet_score_gauge.dart';
import 'widgets/quiet_score_timeline_chart.dart';

class RecommendationDetailPage extends StatefulWidget {
  final int spotId;

  const RecommendationDetailPage({super.key, required this.spotId});

  @override
  State<RecommendationDetailPage> createState() =>
      _RecommendationDetailPageState();
}

class _RecommendationDetailPageState extends State<RecommendationDetailPage> {
  late final _viewModel = RecommendationDetailViewModel(spotId: widget.spotId);
  bool _isFavorite = false; // TODO: 실제 좋아요 API 연결

  @override
  void dispose() {
    _viewModel.dispose();
    super.dispose();
  }

  Future<void> _startVisit(SpotDetail spot) async {
    // TODO: 방문 시작 API(POST /api/visits/start) 호출은 아직 안 함

    // 방문 완료 시 위치로 방문을 인증하므로, 시작 시점에 권한을 확보해 둔다.
    var granted = await LocationPermissionViewModel.isGranted();
    if (!mounted) return;

    if (!granted) {
      // 권한 없으면 허용 화면으로 → 허용받으면 true 반환
      granted = await Get.to<bool>(() => const LocationPermissionPage()) ?? false;
      if (!granted || !mounted) return;
    }

    Get.to(() => VisitingSpotPage(spot: spot));
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: _viewModel,
      builder: (context, _) {
        if (_viewModel.isLoading) {
          return const Scaffold(
            body: SafeArea(child: Center(child: CircularProgressIndicator())),
          );
        }

        if (_viewModel.errorMessage != null) {
          return Scaffold(
            body: SafeArea(
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(_viewModel.errorMessage!),
                    const SizedBox(height: 12),
                    PrimaryButton(
                      label: '다시 시도',
                      isOutlined: true,
                      onPressed: _viewModel.loadDetail,
                    ),
                  ],
                ),
              ),
            ),
          );
        }

        final spot = _viewModel.spot;
        if (spot == null) return const Scaffold(body: SizedBox.shrink());

        return Theme(
          data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
          child: Scaffold(
            body: SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 12, 24, 5),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    IconButton(
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                      onPressed: () => Get.back(),
                      icon: const Icon(Icons.arrow_back, color: Colors.black),
                    ),
                    const SizedBox(height: 10),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: Image.network(
                        spot.imageUrl,
                        width: double.infinity,
                        height: 208,
                        fit: BoxFit.cover,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Align(
                      alignment: AlignmentGeometry.center,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const SizedBox(width: 23), // 하트(15)+간격(8) 만큼 미러 여백
                          Text(
                            spot.name,
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              fontFamily: 'Paperlogy',
                              fontSize: 20,
                              fontWeight: FontWeight.w600,
                              color: Colors.black,
                            ),
                          ),
                          const SizedBox(width: 8),
                          _FavoriteButton(
                            isFavorite: _isFavorite,
                            size: 20,
                            onPressed: () =>
                                setState(() => _isFavorite = !_isFavorite),
                          ),
                        ],
                      ),
                    ),
                    Center(
                      child: Text(
                        spot.address,
                        style: const TextStyle(
                          fontFamily: 'Paperlogy',
                          fontSize: 15,
                          fontWeight: FontWeight.w500,
                          color: Colors.black,
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    Align(
                      alignment: AlignmentGeometry.center,
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: spotModes.map((mode) {
                          return Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: Container(
                              height: 29,
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16,
                              ),
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                color: const Color(0xFFFAFAFA),
                                border: Border.all(
                                  color: const Color(0xFFE5E5E5),
                                ),
                                borderRadius: BorderRadius.circular(45),
                              ),
                              child: Text(
                                mode,
                                style: const TextStyle(
                                  fontFamily: 'Paperlogy',
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                  color: Color(0xFF474444),
                                ),
                              ),
                            ),
                          );
                        }).toList(),
                      ),
                    ),
                    const SizedBox(height: 32),
                    Align(
                      alignment: AlignmentGeometry.center,
                      child: Container(
                        width: 350,
                        child: Text(
                          spot.description,
                          style: const TextStyle(
                            fontWeight: FontWeight.w600,
                            fontFamily: 'Paperlogy',
                            fontSize: 14,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    ),

                    const SizedBox(height: 32),
                    const Divider(height: 24, color: Color(0x33252B28)),
                    const SizedBox(height: 16),
                    const Text(
                      '고요 지수',
                      style: TextStyle(
                        fontFamily: 'Paperlogy',
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Colors.black,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [QuietScoreGauge(quietScore: spot.quietScore)],
                    ),
                    const SizedBox(height: 15),
                    Align(
                      alignment: Alignment.centerRight,
                      child: Text(
                        '${spot.quietScoreUpdatedAt.hour.toString().padLeft(2, '0')}:${spot.quietScoreUpdatedAt.minute.toString().padLeft(2, '0')} 기준',
                        style: const TextStyle(
                          fontFamily: 'Paperlogy',
                          fontSize: 10,
                          color: Color(0xFF7C7C7C),
                        ),
                      ),
                    ),
                    const SizedBox(height: 32),
                    const Text(
                      '고요 지수 타임라인',
                      style: TextStyle(
                        fontFamily: 'Paperlogy',
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Colors.black,
                      ),
                    ),
                    const SizedBox(height: 12),
                    QuietScoreTimelineChart(points: spot.quietScoreTimeline),
                    const SizedBox(height: 32),
                    ReviewSection(
                      reviews: spot.reviews,
                      onSeeAllPressed: () =>
                          Get.to(() => ReviewListPage(reviews: spot.reviews)),
                    ),
                  ],
                ),
              ),
            ),
            persistentFooterButtons: [
              PrimaryButton(
                label: '방문 시작하기',
                onPressed: () => _startVisit(spot),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _FavoriteButton extends StatelessWidget {
  final bool isFavorite;
  final VoidCallback onPressed;
  final double size;

  const _FavoriteButton({
    required this.isFavorite,
    required this.onPressed,
    this.size = 22,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onPressed,
      child: SvgPicture.asset(
        isFavorite
            ? 'assets/icons/heart_filled.svg'
            : 'assets/icons/heart_outline.svg',
        width: size,
        height: size,
      ),
    );
  }
}
