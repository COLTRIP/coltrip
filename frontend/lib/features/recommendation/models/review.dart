class SpotReview {
  final int id;
  final int spotId;
  final int userId;
  final String nickname;
  final int rating; // 1~5
  final String? content; // 선택, 최대 300자 (별점만 남긴 리뷰는 null)
  final DateTime createdAt;

  const SpotReview({
    required this.id,
    required this.spotId,
    required this.userId,
    required this.nickname,
    required this.rating,
    required this.content,
    required this.createdAt,
  });

  factory SpotReview.fromJson(Map<String, dynamic> json) {
    return SpotReview(
      id: json['id'] as int,
      spotId: json['spotId'] as int,
      userId: json['userId'] as int,
      nickname: json['nickname'] as String,
      rating: json['rating'] as int,
      content: json['content'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
