// GET /api/spots/{spotId}/alternatives 응답의 alternatives[] 항목 하나 (대체지 하나)
class AlternativeSpot {
  final AlternativeSpotInfo spot;
  final double? quietIndex;
  final double? distanceKm;
  final double? score;
  final String recommendReason;

  const AlternativeSpot({
    required this.spot,
    required this.quietIndex,
    required this.distanceKm,
    required this.score,
    required this.recommendReason,
  });

  factory AlternativeSpot.fromJson(Map<String, dynamic> json) {
    return AlternativeSpot(
      spot: AlternativeSpotInfo.fromJson(json['spot'] as Map<String, dynamic>),
      quietIndex: (json['quietIndex'] as num?)?.toDouble(),
      distanceKm: (json['distanceKm'] as num?)?.toDouble(),
      score: (json['score'] as num?)?.toDouble(),
      recommendReason: json['recommendReason'] as String? ?? '',
    );
  }
}

// alternatives 응답 안의 spot은 고요지수를 포함하지 않는다.
// AI가 계산한 대체지 고요지수는 상위 Alternative.quietIndex에 담긴다.
class AlternativeSpotInfo {
  final int id;
  final String name;
  final String category;
  final double latitude;
  final double longitude;

  const AlternativeSpotInfo({
    required this.id,
    required this.name,
    required this.category,
    required this.latitude,
    required this.longitude,
  });

  factory AlternativeSpotInfo.fromJson(Map<String, dynamic> json) {
    return AlternativeSpotInfo(
      id: json['id'] as int,
      name: json['name'] as String,
      category: json['category'] as String,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
    );
  }
}
