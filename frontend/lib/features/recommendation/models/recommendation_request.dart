class RecommendationRequest {
  const RecommendationRequest({
    required this.placeTypeId,
    required this.mood,
  });

  final String placeTypeId;
  final String mood;

  Map<String, dynamic> toJson() {
    return {
      'placeTypeId': placeTypeId,
      'mood': mood,
    };
  }
}
