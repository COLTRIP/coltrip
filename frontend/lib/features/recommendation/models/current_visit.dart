import 'recommendation.dart';

class CurrentVisit {
  final int visitId;
  final int spotId;
  final String spotName;
  final String spotAddress;
  final String category;
  final String? imageUrl;
  final double latitude;
  final double longitude;
  final String status;
  final DateTime startedAt;
  final int? startQuietScore;
  final int? currentQuietScore;
  final String? currentQuietLevel;
  final int visitRadiusMeters;

  const CurrentVisit({
    required this.visitId,
    required this.spotId,
    required this.spotName,
    required this.spotAddress,
    required this.category,
    required this.imageUrl,
    required this.latitude,
    required this.longitude,
    required this.status,
    required this.startedAt,
    required this.startQuietScore,
    required this.currentQuietScore,
    required this.currentQuietLevel,
    required this.visitRadiusMeters,
  });

  factory CurrentVisit.fromJson(Map<String, dynamic> json) {
    return CurrentVisit(
      visitId: json['visitId'] as int,
      spotId: json['spotId'] as int,
      spotName: json['spotName'] as String,
      spotAddress: json['spotAddress'] as String,
      category: json['category'] as String,
      imageUrl: json['imageUrl'] as String?,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      status: json['status'] as String,
      startedAt: DateTime.parse(json['startedAt'] as String),
      startQuietScore: json['startQuietScore'] as int?,
      currentQuietScore: json['currentQuietScore'] as int?,
      currentQuietLevel: json['currentQuietLevel'] as String?,
      visitRadiusMeters: json['visitRadiusMeters'] as int,
    );
  }

  SpotDetail toPlaceholderSpotDetail() {
    return SpotDetail(
      id: spotId,
      name: spotName,
      address: spotAddress,
      category: category,
      modes: const [],
      description: '',
      imageUrl: imageUrl ?? '',
      recommendReason: '',
      latitude: latitude,
      longitude: longitude,
      quietScore: currentQuietScore,
      quietLevel: currentQuietLevel,
      quietScoreUpdatedAt: startedAt,
      isLiked: false,
    );
  }
}
