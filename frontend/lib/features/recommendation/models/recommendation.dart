import 'quiet_score_point.dart';
import 'review.dart';

// TODO: 이전 화면에서 받은 감성 모드 받아와서 띄우기
const spotModes = ['ASMR', '풍경위주', '적막함'];

class Spot {
  final int id;
  final String name;
  final String category;
  final List<String> modes;
  final double latitude;
  final double longitude;
  final int quietScore;
  final DateTime quietScoreUpdatedAt;
  final String address; // TODO: 목록 API 응답에 아직 없음(docs/api.md)
  final String imageUrl; // TODO: 위와 동일

  const Spot({
    required this.id,
    required this.name,
    required this.category,
    required this.modes,
    required this.latitude,
    required this.longitude,
    required this.quietScore,
    required this.quietScoreUpdatedAt,
    required this.address,
    required this.imageUrl,
  });

  factory Spot.fromJson(Map<String, dynamic> json) {
    return Spot(
      id: json['id'] as int,
      name: json['name'] as String,
      category: json['category'] as String,
      modes: (json['modes'] as List).cast<String>(),
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      quietScore: json['quietScore'] as int,
      quietScoreUpdatedAt: DateTime.parse(json['quietScoreUpdatedAt'] as String),
      address: json['address'] as String,
      imageUrl: json['imageUrl'] as String,
    );
  }
}

class SpotDetail {
  final int id;
  final String name;
  final String address;
  final String category;
  final List<String> modes;
  final String description;
  final String imageUrl;
  final String recommendReason;
  final double latitude;
  final double longitude;
  final int quietScore;
  final DateTime quietScoreUpdatedAt;
  // TODO: 아래 두 필드는 docs/api.md에 없는 데이터 (리뷰 API, 고요지수 타임라인 API 자체가 아직 없음) — 백엔드 확인 필요
  final List<QuietScorePoint> quietScoreTimeline;
  final List<SpotReview> reviews;

  const SpotDetail({
    required this.id,
    required this.name,
    required this.address,
    required this.category,
    required this.modes,
    required this.description,
    required this.imageUrl,
    required this.recommendReason,
    required this.latitude,
    required this.longitude,
    required this.quietScore,
    required this.quietScoreUpdatedAt,
    required this.quietScoreTimeline,
    required this.reviews,
  });

  factory SpotDetail.fromJson(Map<String, dynamic> json) {
    return SpotDetail(
      id: json['id'] as int,
      name: json['name'] as String,
      address: json['address'] as String,
      category: json['category'] as String,
      modes: (json['modes'] as List).cast<String>(),
      description: json['description'] as String,
      imageUrl: json['imageUrl'] as String,
      recommendReason: json['recommendReason'] as String,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      quietScore: json['quietScore'] as int,
      quietScoreUpdatedAt: DateTime.parse(json['quietScoreUpdatedAt'] as String),
      quietScoreTimeline: const [], // 실제 API 없어서 빈 값으로 기본 처리
      reviews: const [],
    );
  }
}
