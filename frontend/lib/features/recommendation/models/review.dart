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
    final id = json['id'];
    final spotId = json['spotId'];
    final userId = json['userId'];
    final rating = json['rating'];
    final createdAt = json['createdAt'];

    if (id is! num ||
        spotId is! num ||
        userId is! num ||
        rating is! num ||
        createdAt is! String) {
      throw const FormatException('리뷰 응답 형식이 올바르지 않습니다.');
    }

    return SpotReview(
      id: id.toInt(),
      spotId: spotId.toInt(),
      userId: userId.toInt(),
      nickname: (json['nickname'] as String?)?.trim().isNotEmpty == true
          ? (json['nickname'] as String).trim()
          : '사용자',
      rating: rating.toInt(),
      content: json['content'] as String?,
      createdAt: DateTime.parse(createdAt),
    );
  }
}
