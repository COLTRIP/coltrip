class RecommendationLoadException implements Exception {
  final String message;

  const RecommendationLoadException([
    this.message = '추천 목록을 불러오지 못했어요. 다시 시도해주세요.',
  ]);

  @override
  String toString() => message;
}
