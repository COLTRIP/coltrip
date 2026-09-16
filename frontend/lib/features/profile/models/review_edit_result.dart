/// 리뷰 수정 결과를 전달하기 위한 모델입니다.
///
/// 수정된 평점과 리뷰 내용을 포함합니다.
class ReviewEditResult {
  const ReviewEditResult({required this.rating, this.content});

  final int rating;
  final String? content;
}
