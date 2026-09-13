class Place {
  const Place({
    required this.name,
    required this.imageUrl,
    this.id,
    this.address,
    this.rating,
    this.visitedAt,
    this.isLiked = false,
  });

  final int? id;
  final String name;
  final String imageUrl;
  final String? address;
  final double? rating;
  final DateTime? visitedAt;
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
}
