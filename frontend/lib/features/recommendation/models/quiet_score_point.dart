// 특정 시각의 예상 고요지수 (일주일 그래프용)
class QuietScorePoint {
  final DateTime targetAt;
  final int? score;

  const QuietScorePoint({required this.targetAt, required this.score});

  factory QuietScorePoint.fromTimelineJson(Map<String, dynamic> json) {
    final targetAt = DateTime.parse(json['targetAt'] as String).toLocal();
    return QuietScorePoint(
      targetAt: targetAt,
      score: (json['quietIndex'] as num?)?.round(),
    );
  }
}
