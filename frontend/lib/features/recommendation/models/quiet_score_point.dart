// 특정 시각의 고요지수 (고요지수 타임라인 막대그래프용)
class QuietScorePoint {
  final int hour;
  final int score;

  const QuietScorePoint({
    required this.hour,
    required this.score,
  });

  factory QuietScorePoint.fromTimelineJson(Map<String, dynamic> json) {
    final targetAt = DateTime.parse(json['targetAt'] as String).toLocal();
    return QuietScorePoint(
      hour: targetAt.hour,
      score: (json['quietIndex'] as num).round(),
    );
  }
}