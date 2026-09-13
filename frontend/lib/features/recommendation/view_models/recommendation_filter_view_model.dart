import 'package:flutter/foundation.dart';

import '../models/recommendation_filter.dart';

class RecommendationFilterViewModel extends ChangeNotifier {
  RecommendationFilter filter;

  RecommendationFilterViewModel({
    DateTime? initialDateTime,
    required String category,
  }) : filter = RecommendationFilter(
         dateTime: initialDateTime ?? DateTime.now(),
         category: category,
       );

  void updateDateTime(DateTime dateTime) {
    filter = RecommendationFilter(
      dateTime: dateTime,
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

  String get formattedTime {
    final dt = filter.dateTime;
    final hour12 = dt.hour % 12 == 0 ? 12 : dt.hour % 12;
    final period = dt.hour < 12 ? '오전' : '오후';
    final minute = dt.minute.toString().padLeft(2, '0');
    return '$period $hour12시 $minute분';
  }
}
