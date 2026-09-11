// TODO: 이전 화면에서 받은 감성 모드 받아와서 띄우기
const spotModes = ['ASMR', '풍경위주', '적막함'];

class Spot {
  final int id;
  final String name;
  final String address;
  final String category;
  final List<String> modes;
  final String? imageUrl; // 목록 API 응답에서 아직 null로 옴
  final double latitude;
  final double longitude;
  final int? quietScore;
  final String? quietLevel;
  final DateTime quietScoreUpdatedAt;


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
    required this.quietLevel,
  });

  factory Spot.fromJson(Map<String, dynamic> json) {
    return Spot(
      id: json['id'] as int,
      name: json['name'] as String,
      category: json['category'] as String,
      modes: (json['modes'] as List).cast<String>(),
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      quietScore: json['quietScore'] as int?,
      quietLevel : json['quietLevel'] as String?,
      quietScoreUpdatedAt: DateTime.parse(json['quietScoreUpdatedAt'] as String),
      address: json['address'] as String,
      imageUrl: json['imageUrl'] as String?,
    );
  }
}

class SpotDetail {
  final int id;
  final String name;
  final String address;
  final String category;
  final List<String>? modes;
  final String description;
  final String? imageUrl;
  final String recommendReason;
  final double latitude;
  final double longitude;
  final int? quietScore;
  final String? quietLevel;
  final DateTime? quietScoreUpdatedAt; // quietScore 미계산 스팟은 null
  final bool isLiked;

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
    required this.quietLevel,
    required this.quietScoreUpdatedAt,
    required this.isLiked,
  });

  factory SpotDetail.fromJson(Map<String, dynamic> json) {
    return SpotDetail(
      id: json['id'] as int,
      name: json['name'] as String,
      address: json['address'] as String,
      category: json['category'] as String,
      modes: (json['modes'] as List?)?.cast<String>(),
      description: json['description'] as String,
      imageUrl: json['imageUrl'] as String?,
      recommendReason: json['recommendReason'] as String,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      quietScore: json['quietScore'] as int?,
      quietScoreUpdatedAt: json['quietScoreUpdatedAt'] == null
          ? null
          : DateTime.parse(json['quietScoreUpdatedAt'] as String),
      quietLevel: json['quietLevel'] as String?,
      isLiked: json['isLiked'] as bool? ?? false,
    );
  }

  SpotDetail copyWith({bool? isLiked}) {
    return SpotDetail(
      id: id,
      name: name,
      address: address,
      category: category,
      modes: modes,
      description: description,
      imageUrl: imageUrl,
      recommendReason: recommendReason,
      latitude: latitude,
      longitude: longitude,
      quietScore: quietScore,
      quietLevel: quietLevel,
      quietScoreUpdatedAt: quietScoreUpdatedAt,
      isLiked: isLiked ?? this.isLiked,
    );
  }
}
