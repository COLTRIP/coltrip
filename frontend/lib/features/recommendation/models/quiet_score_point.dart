// 특정 시각의 고요지수 (고요지수 타임라인 막대그래프용)
class QuietScorePoint {
  final int hour; // 0~23
  final int score; // 0~100

  const QuietScorePoint({required this.hour, required this.score});
}
