import 'package:flutter/foundation.dart';

import '../models/recommendation_filter.dart';

class RecommendationFilterViewModel extends ChangeNotifier {
  RecommendationFilter filter;
  bool isCurrent = true;

  RecommendationFilterViewModel({
    DateTime? initialDateTime,
    required String category,
  }) : filter = RecommendationFilter(
         dateTime: initialDateTime ?? DateTime.now(),
         category: category,
       );

  void updateDate(DateTime date) {
    final fixedTime = filter.dateTime;
    isCurrent = false;
    filter = RecommendationFilter(
      dateTime: DateTime(date.year, date.month, date.day, fixedTime.hour),
      category: filter.category,
    );
    notifyListeners();
  }

  void updateCategory(String category) {
    filter = RecommendationFilter(
      dateTime: filter.dateTime,
      category: category,
    );
    notifyListeners();
  }

  String get formattedDate {
    final dt = filter.dateTime;
    return '${dt.year}년 ${dt.month}월 ${dt.day}일';
  }
}
