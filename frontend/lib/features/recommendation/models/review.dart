class SpotReview {
  final String userName;
  final int rating; // 1~5
  final DateTime date;
  final String content;

  const SpotReview({
    required this.userName,
    required this.rating,
    required this.date,
    required this.content,
  });
}
