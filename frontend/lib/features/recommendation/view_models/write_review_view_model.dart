import 'package:flutter/material.dart';

import '../../../core/network/api_exception.dart';
import '../repositories/review_repository.dart';

class WriteReviewViewModel extends ChangeNotifier {
  WriteReviewViewModel({
    required this.visitId,
    ReviewRepository? repository,
  }) : _repository = repository ?? ReviewRepository();

  final int visitId;
  final ReviewRepository _repository;

  final TextEditingController contentController = TextEditingController();

  int? rating; // 1~5, 필수
  bool isSubmitting = false;
  String? errorMessage;

  bool get canSubmit => rating != null && !isSubmitting;

  void setRating(int value) {
    rating = value;
    errorMessage = null;
    notifyListeners();
  }

  /// 성공 시 true. 호출측에서 화면을 닫고 리뷰 목록을 새로고침한다.
  Future<bool> submit() async {
    if (rating == null || isSubmitting) return false;

    isSubmitting = true;
    errorMessage = null;
    notifyListeners();

    try {
      final content = contentController.text.trim();
      await _repository.createReview(
        visitId: visitId,
        rating: rating!,
        content: content.isEmpty ? null : content,
      );
      return true;
    } on ApiException catch (e) {
      errorMessage = switch (e.code) {
        'ReviewNotAllowedException' => '이미 리뷰를 작성했거나 방문이 완료되지 않았어요.',
        'ValidationException' => '별점을 선택해주세요.',
        'VisitNotFoundException' => '방문 정보를 찾을 수 없어요.',
        _ => e.message,
      };
      return false;
    } catch (_) {
      errorMessage = '리뷰 작성에 실패했어요. 다시 시도해주세요.';
      return false;
    } finally {
      isSubmitting = false;
      notifyListeners();
    }
  }

  @override
  void dispose() {
    contentController.dispose();
    super.dispose();
  }
}
