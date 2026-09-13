import 'package:flutter/material.dart';

import '../../../../shared/widgets/shared_app_bar.dart';
import '../../data/place_mood_data.dart';
import '../../view_models/recommendation_filter_view_model.dart';
import '../../view_models/recommendation_view_model.dart';
import 'widgets/recommendation_card.dart';
import 'widgets/recommendation_filter_sheet.dart';
import '../../../../shared/widgets/app_button.dart';

class RecommendationPage extends StatefulWidget {
  // 이전 화면에서 이미 고른 카테고리를 전달받음
  final String? category;
  final String? categoryLabel;
  final List<String> modes;

  const RecommendationPage({
    super.key,
    this.category,
    this.categoryLabel,
    this.modes = const [],
  });

  @override
  State<RecommendationPage> createState() => _RecommendationPageState();
}

class _RecommendationPageState extends State<RecommendationPage> {
  final _viewModel = RecommendationViewModel();
  late final _filterViewModel = RecommendationFilterViewModel(
    category: widget.category ?? '전체',
  );

  @override
  void initState() {
    super.initState();
    _filterViewModel.addListener(_onFilterChanged);
    _loadSpots();
  }

  void _onFilterChanged() {
    _loadSpots(); // 필터 바뀌면 추천 목록 다시 불러오기
  }

  void _loadSpots() {
    _viewModel.loadRecommendations(
      dateTime: _filterViewModel.filter.dateTime,
      category: _filterViewModel.filter.category == '전체'
          ? null
          : _filterViewModel.filter.category,
      modes: widget.modes,
    );
  }

  String _moodLabel(String id) {
    for (final mood in PlaceMoodData.items) {
      if (mood.id == id) return mood.label;
    }
    return id;
  }

  String get _categoryLabel {
    if (widget.categoryLabel != null) return widget.categoryLabel!;

    return switch (widget.category) {
      'CAFE' => '☕ 카페',
      'PARK' => '🌲 자연 · 공원',
      'GALLERY' => '🖼️ 문화시설',
      'ALLEY' => '🏙️ 도심 · 랜드마크',
      'TEMPLE' => '⛩ 역사 · 종교',
      'LIBRARY' || 'BOOKSTORE' => '📚 도서관 · 서점',
      'BEACH' => '🌊 해변',
      _ => '전체',
    };
  }

  @override
  void dispose() {
    _filterViewModel.removeListener(_onFilterChanged);
    _viewModel.dispose();
    _filterViewModel.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const SharedAppBar(title: '추천 장소', showBackButton: true),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 12),
              child: ListenableBuilder(
                listenable: _filterViewModel,
                builder: (context, _) => RecommendationFilterSheet(
                  viewModel: _filterViewModel,
                  categoryLabel: _categoryLabel,
                ),
              ),
            ),
            const SizedBox(height: 12),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '감성',
                    style: TextStyle(
                      fontFamily: 'Paperlogy',
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: Colors.black,
                    ),
                  ),
                  const SizedBox(height: 10),
                  SizedBox(
                    height: 40,
                    child: ListView.separated(
                      scrollDirection: Axis.horizontal,
                      physics: const BouncingScrollPhysics(),
                      itemCount: widget.modes.length,
                      separatorBuilder: (_, _) => const SizedBox(width: 10),
                      itemBuilder: (context, index) {
                        final mode = widget.modes[index];

                        return Container(
                          constraints: const BoxConstraints(minWidth: 88),
                          padding: const EdgeInsets.symmetric(horizontal: 20),
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            color: const Color(0xFFFAFAFA),
                            border: Border.all(
                              color: const Color(0xFFE1E3E2),
                              width: 1.2,
                            ),
                            borderRadius: BorderRadius.circular(24),
                          ),
                          child: Text(
                            _moodLabel(mode),
                            style: const TextStyle(
                              fontFamily: 'Paperlogy',
                              fontSize: 14,
                              fontWeight: FontWeight.w400,
                              color: Color(0xFF474444),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            ), // 감성 모드
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 20),
              child: Divider(color: Color(0x33252B28)),
            ),
            const SizedBox(height: 15),
            Expanded(
              child: ListenableBuilder(
                listenable: _viewModel,
                builder: (context, _) {
                  if (_viewModel.isLoading) {
                    return const Center(child: CircularProgressIndicator());
                  }

                  if (_viewModel.errorMessage != null) {
                    return Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(_viewModel.errorMessage!),
                          const SizedBox(height: 12),
                          PrimaryButton(
                            label: '다시 시도',
                            isOutlined: true,
                            onPressed: _loadSpots,
                          ),
                        ],
                      ),
                    );
                  }

                  final spots = _viewModel.spots;
                  if (spots.isEmpty) {
                    return Center(
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 32),
                        child: Text(
                          _viewModel.emptyMessage ?? '추천할 장소가 없어요.',
                          textAlign: TextAlign.center,
                        ),
                      ),
                    );
                  }

                  return ListView.separated(
                    itemCount: spots.length,
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                    itemBuilder: (context, index) {
                      return RecommendationCard(spot: spots[index]);
                    },
                    separatorBuilder: (context, index) {
                      return const SizedBox(height: 16);
                    },
                  );
                },
              ),
            ), //카드
          ],
        ),
      ),
    );
  }
}
