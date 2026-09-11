import 'package:coltrip/features/recommendation/views/review_writing/widget/text_review.dart';
import 'package:coltrip/shared/widgets/app_button.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../models/recommendation.dart';
import '../../view_models/write_review_view_model.dart';
import '../visiting_spot/widgets/visiting_spot_card.dart';
import 'widget/satisfaction_section.dart';

class WriteReviewPage extends StatefulWidget {
  final SpotDetail spot;
  final int visitId;

  const WriteReviewPage({super.key, required this.spot, required this.visitId});

  @override
  State<WriteReviewPage> createState() => _WriteReviewPageState();
}

class _WriteReviewPageState extends State<WriteReviewPage> {
  late final _viewModel = WriteReviewViewModel(visitId: widget.visitId);

  @override
  void dispose() {
    _viewModel.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final ok = await _viewModel.submit();
    if (ok && mounted) {
      // 상세 화면에서 리뷰 목록 새로고침하도록 true 반환
      Get.back(result: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: ListenableBuilder(
            listenable: _viewModel,
            builder: (context, _) {
              return SingleChildScrollView(
                child: Column(
                  children: [
                    Align(
                      alignment: AlignmentGeometry.topLeft,
                      child: IconButton(
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        onPressed: () => Get.back(),
                        icon: const Icon(Icons.arrow_back, color: Colors.black),
                      ),
                    ),
                    const SizedBox(height: 10),
                    VisitingSpotCard(spot: widget.spot),
                    const SizedBox(height: 30),
                    const Text(
                      '방문했던 곳, 어떠셨나요?\n리뷰를 남겨주세요!',
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        fontFamily: 'Paperlogy',
                        fontSize: 16,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 20),
                    const Divider(height: 24, color: Color(0x33252B28)),
                    const SizedBox(height: 16),
                    SatisfactionSection(
                      selected: _viewModel.rating,
                      onSelected: _viewModel.setRating,
                    ),
                    const SizedBox(height: 32),
                    TextReview(controller: _viewModel.contentController),
                    const SizedBox(height: 24),
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
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: PrimaryButton(
                        label: '작성 완료',
                        isLoading: _viewModel.isSubmitting,
                        onPressed: _viewModel.canSubmit ? _submit : null,
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}
