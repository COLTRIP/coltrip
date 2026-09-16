/// 프로필 화면에서 사용하는 장소 정보를 나타내는 모델입니다.
///
/// 장소의 기본 정보와 좋아요 여부, 방문 및 리뷰 관련 정보를 관리합니다.
class Place {
  const Place({
    required this.name,
    required this.imageUrl,
    this.id,
    this.address,
    this.rating,
    this.visitedAt,
    this.reviewId,
    this.isLiked = false,
  });

  final int? id;
  final String name;
  final String imageUrl;
  final String? address;
  final double? rating;
  final DateTime? visitedAt;
  final int? reviewId;
  final bool isLiked;

  factory Place.fromSpotJson(Map<String, dynamic> json) {
    return Place(
      id: (json['id'] as num?)?.toInt(),
      name: json['name'] as String? ?? '이름 없는 장소',
      address: json['address'] as String?,
      imageUrl: json['imageUrl'] as String? ?? '',
      isLiked: json['isLiked'] as bool? ?? false,
    );
  }

  factory Place.fromVisitHistoryJson(Map<String, dynamic> json) {
    final completedAt = json['completedAt'] as String?;

    return Place(
      id: (json['spotId'] as num?)?.toInt(),
      name: json['spotName'] as String? ?? '이름 없는 장소',
      address: json['spotAddress'] as String?,
      imageUrl: json['imageUrl'] as String? ?? '',
      visitedAt: completedAt == null ? null : DateTime.tryParse(completedAt),
      reviewId: (json['reviewId'] as num?)?.toInt(),
    );
  }
}
