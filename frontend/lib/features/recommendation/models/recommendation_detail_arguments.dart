class RecommendationDetailArguments {
  const RecommendationDetailArguments({
    required this.spotId,
    required this.predictedQuietScore,
    required this.predictionTargetAt,
  });

  final int spotId;
  final int? predictedQuietScore;
  final DateTime? predictionTargetAt;
}
