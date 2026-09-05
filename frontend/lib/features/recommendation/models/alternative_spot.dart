// GET /api/spots/{spotId}/alternatives 응답의 alternatives[] 항목 하나 (대체지 하나)
class AlternativeSpot {
  final AlternativeSpotInfo spot;
  final double similarityScore;
  final String recommendReason;

  const AlternativeSpot({
    required this.spot,
    required this.similarityScore,
    required this.recommendReason,
  });

  factory AlternativeSpot.fromJson(Map<String, dynamic> json) {
    return AlternativeSpot(
      spot: AlternativeSpotInfo.fromJson(json['spot'] as Map<String, dynamic>),
      similarityScore: (json['similarityScore'] as num).toDouble(),
      recommendReason: json['recommendReason'] as String,
    );
  }
}

// alternatives 응답 안의 spot은 목록(/api/spots)·상세(/api/spots/{id}) 스키마와 다르게
// id/name/category/latitude/longitude/quietScore만 옴 (modes, quietScoreUpdatedAt 없음)
class AlternativeSpotInfo {
  final int id;
  final String name;
  final String category;
  final double latitude;
  final double longitude;
  final int quietScore;

  const AlternativeSpotInfo({
    required this.id,
    required this.name,
    required this.category,
    required this.latitude,
    required this.longitude,
    required this.quietScore,
  });

  factory AlternativeSpotInfo.fromJson(Map<String, dynamic> json) {
    return AlternativeSpotInfo(
      id: json['id'] as int,
      name: json['name'] as String,
      category: json['category'] as String,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      quietScore: json['quietScore'] as int,
    );
  }
}
