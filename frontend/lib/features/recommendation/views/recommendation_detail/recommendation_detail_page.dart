import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../../../app/routes/app_routes.dart';
import '../../../../shared/widgets/primary_button.dart';
import '../../data/place_mood_data.dart';
import '../../models/recommendation.dart';
import '../../view_models/location_permission_view_model.dart';
import '../../view_models/recommendation_detail_view_model.dart';
import '../../view_models/review_view_model.dart';
import '../review/widgets/review_section.dart';
import 'widgets/quiet_score_gauge.dart';
import 'widgets/quiet_score_timeline_chart.dart';

class RecommendationDetailPage extends StatefulWidget {
  final int spotId;
  final int? predictedQuietScore;
  final DateTime? predictionTargetAt;

  const RecommendationDetailPage({
    super.key,
    required this.spotId,
    this.predictedQuietScore,
    this.predictionTargetAt,
  });

  @override
  State<RecommendationDetailPage> createState() =>
      _RecommendationDetailPageState();
}

class _RecommendationDetailPageState extends State<RecommendationDetailPage> {
  late final _viewModel = RecommendationDetailViewModel(
    spotId: widget.spotId,
    timelineDateTime: widget.predictionTargetAt,
  );
  late final _reviewViewModel = ReviewViewModel(spotId: widget.spotId);

  @override
  void dispose() {
    _viewModel.dispose();
    _reviewViewModel.dispose();
    super.dispose();
  }

  Future<void> _startVisit(SpotDetail spot) async {
    final hasLocationPermission = await LocationPermissionViewModel.isGranted();
    if (!hasLocationPermission) {
      final result = await Get.toNamed(AppRoutes.locationPermission);
      if (result is! bool || !result || !mounted) return;
    }

    // 리뷰 작성까지 마치고 돌아오면 true → 리뷰 목록 새로고침
    final visitingResult = await Get.toNamed(
      AppRoutes.visitingSpot,
      arguments: spot,
    );
    final reviewed = visitingResult as bool?;
    if (reviewed == true && mounted) {
      _reviewViewModel.loadReviews();
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Listenable.merge([_viewModel, _reviewViewModel]),
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
                    Text(
                      _viewModel.errorMessage!,
                      style: const TextStyle(fontSize: 16),
                    ),
                    const SizedBox(height: 20),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 20),
                      child: PrimaryButton(
                        label: '다시 시도',
                        isOutlined: true,
                        onPressed: _viewModel.loadDetail,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }

        final spot = _viewModel.spot;
        if (spot == null) return const Scaffold(body: SizedBox.shrink());
        final isForecast = widget.predictionTargetAt != null;
        final displayedQuietScore =
            widget.predictedQuietScore ?? spot.quietScore ?? 0;
        final displayedAt = isForecast
            ? widget.predictionTargetAt
            : spot.quietScoreUpdatedAt;

        return Theme(
          data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
          child: Scaffold(
            body: SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 12, 24, 5),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Transform.translate(
                      offset: const Offset(-8, 0),
                      child: IconButton(
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        onPressed: () => Get.back(),
                        icon: const Icon(Icons.arrow_back, color: Colors.black),
                      ),
                    ),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: (spot.imageUrl == null || spot.imageUrl!.isEmpty)
                          ? const _UnavailablePlaceImage()
                          : Image.network(
                              spot.imageUrl!,
                              width: double.infinity,
                              height: 208,
                              fit: BoxFit.cover,
                              errorBuilder: (_, _, _) {
                                return const _UnavailablePlaceImage();
                              },
                            ),
                    ),
                    if (spot.imageUrl != null && spot.imageUrl!.isNotEmpty) ...[
                      const SizedBox(height: 7),
                      const Align(
                        alignment: Alignment.centerRight,
                        child: Text(
                          '사진 제공: 한국관광공사',
                          style: TextStyle(
                            fontSize: 7,
                            fontWeight: FontWeight.w300,
                            color: Color(0xFF8A918E),
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(height: 14),
                    Align(
                      alignment: AlignmentGeometry.center,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const SizedBox(height: 10),
                          Text(
                            spot.name,
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w600,
                              color: Colors.black,
                            ),
                          ),
                          const SizedBox(width: 8),
                          _FavoriteButton(
                            isFavorite: spot.isLiked,
                            size: 30,
                            onPressed: _viewModel.toggleLike,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 5),
                    Center(
                      child: Text(
                        spot.address,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w400,
                          color: Colors.black,
                        ),
                      ),
                    ),
                    if (spot.modes != null && spot.modes!.isNotEmpty) ...[
                      const SizedBox(height: 12),
                      Align(
                        alignment: AlignmentGeometry.center,
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: spot.modes!.map((mode) {
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
                                  PlaceMoodData.labelFor(mode),
                                  style: const TextStyle(
                                    fontSize: 12,
                                    fontWeight: FontWeight.w500,
                                    color: Color(0xFF474444),
                                  ),
                                ),
                              ),
                            );
                          }).toList(),
                        ),
                      ),
                    ],
                    const SizedBox(height: 20),
                    Align(
                      alignment: AlignmentGeometry.center,
                      child: SizedBox(
                        width: 350,
                        child: Text(
                          spot.description,
                          style: const TextStyle(
                            fontWeight: FontWeight.w400,
                            fontSize: 12,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    ),

                    const SizedBox(height: 12),
                    const Divider(height: 24, color: Color(0x33252B28)),
                    const SizedBox(height: 12),
                    Text(
                      isForecast ? '예상 고요 지수' : '현재 고요 지수',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Colors.black,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        QuietScoreGauge(quietScore: displayedQuietScore),
                      ],
                    ),
                    const SizedBox(height: 20),
                    const Center(
                      child: Text(
                        '혼잡도 데이터 제공: SK텔레콤 지오비전 퍼즐',
                        style: TextStyle(
                          fontSize: 7,
                          fontWeight: FontWeight.w300,
                          color: Color(0xFF8A918E),
                        ),
                      ),
                    ),
                    if (displayedAt != null) ...[
                      const SizedBox(height: 10),
                      Align(
                        alignment: Alignment.centerRight,
                        child: Text(
                          isForecast
                              ? '${displayedAt.month}/${displayedAt.day} '
                                    '${displayedAt.hour.toString().padLeft(2, '0')}:00 예상'
                              : '${displayedAt.hour.toString().padLeft(2, '0')}:'
                                    '${displayedAt.minute.toString().padLeft(2, '0')} 기준',
                          style: const TextStyle(
                            fontSize: 10,
                            color: Color(0xFF7C7C7C),
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(height: 32),
                    const Text(
                      '주간 고요 지수',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Colors.black,
                      ),
                    ),
                    const SizedBox(height: 12),
                    QuietScoreTimelineChart(points: _viewModel.timelinePoints),
                    const SizedBox(height: 32),
                    ReviewSection(
                      reviews: _reviewViewModel.reviews,
                      onSeeAllPressed: () => Get.toNamed(
                        AppRoutes.reviewList,
                        arguments: _reviewViewModel.reviews,
                      ),
                    ),
                    const SizedBox(height: 10),
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
    this.size = 30,
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

class _UnavailablePlaceImage extends StatelessWidget {
  const _UnavailablePlaceImage();

  @override
  Widget build(BuildContext context) {
    return const SizedBox(
      width: double.infinity,
      height: 208,
      child: ColoredBox(
        color: Color(0xFFE9ECEF),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.image_not_supported_outlined,
              size: 42,
              color: Color(0xFF8A918E),
            ),
            SizedBox(height: 8),
            Text(
              '이미지를 불러올 수 없어요.',
              style: TextStyle(fontSize: 13, color: Color(0xFF737B77)),
            ),
          ],
        ),
      ),
    );
  }
}
