class PlaceReview {
  const PlaceReview({
    required this.id,
    required this.rating,
    required this.content,
    required this.createdAt,
  });

  final int id;
  final double rating;
  final String content;
  final DateTime createdAt;

  factory PlaceReview.fromJson(Map<String, dynamic> json) {
    return PlaceReview(
      id: (json['id'] as num).toInt(),
      rating: (json['rating'] as num).toDouble(),
      content: json['content'] as String? ?? '',
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
