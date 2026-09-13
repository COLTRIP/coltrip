class MapSpot {
  const MapSpot({
    required this.id,
    required this.name,
    required this.latitude,
    required this.longitude,
    this.address,
    this.category,
    this.imageUrl,
    this.quietScore,
    this.quietLevel,
    this.isLiked = false,
  });

  final int id;
  final String name;
  final double latitude;
  final double longitude;
  final String? address;
  final String? category;
  final String? imageUrl;
  final int? quietScore;
  final String? quietLevel;
  final bool isLiked;

  factory MapSpot.fromJson(Map<String, dynamic> json) {
    return MapSpot(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String? ?? '이름 없는 장소',
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      address: json['address'] as String?,
      category: json['category'] as String?,
      imageUrl: json['imageUrl'] as String?,
      quietScore: (json['quietScore'] as num?)?.toInt(),
      quietLevel: json['quietLevel'] as String?,
      isLiked: json['isLiked'] as bool? ?? false,
    );
  }
}
