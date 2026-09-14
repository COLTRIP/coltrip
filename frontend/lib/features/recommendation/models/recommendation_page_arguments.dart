class RecommendationPageArguments {
  const RecommendationPageArguments({
    required this.category,
    required this.categoryLabel,
    required this.modes,
  });

  final String? category;
  final String categoryLabel;
  final List<String> modes;
}
