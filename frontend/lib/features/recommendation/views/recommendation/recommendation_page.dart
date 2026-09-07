import 'package:flutter/material.dart';

import '../../models/recommendation.dart';
import '../../view_models/recommendation_filter_view_model.dart';
import '../../view_models/recommendation_view_model.dart';
import 'widgets/recommendation_card.dart';
import 'widgets/recommendation_filter_sheet.dart';
import '../../../../shared/widgets/app_button.dart';

class RecommendationPage extends StatefulWidget {
  // 이전 화면에서 이미 고른 카테고리를 전달받음
  final String category;

  const RecommendationPage({super.key, required this.category});

  @override
  State<RecommendationPage> createState() => _RecommendationPageState();
}

class _RecommendationPageState extends State<RecommendationPage> {
  final _viewModel = RecommendationViewModel();
  late final _filterViewModel = RecommendationFilterViewModel(category: widget.category);

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
    _viewModel.loadSpots(category: _filterViewModel.filter.category);
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
      body: SafeArea(
        child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [

          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 12),
            child: ListenableBuilder(
              listenable: _filterViewModel,
              builder: (context, _) => RecommendationFilterSheet(viewModel: _filterViewModel),
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
                Row(
                  children: spotModes.map((mode) {
                    return Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: Container(
                        height: 29,
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: const Color(0xFFFAFAFA),
                          border: Border.all(color: const Color(0xFFE5E5E5)),
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
                  return const Center(child: Text('추천할 장소가 없어요.'));
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
