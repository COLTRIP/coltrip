import 'package:flutter/material.dart';

class PlaceTypeItem {
  const PlaceTypeItem({
    required this.id,
    required this.apiCategory,
    required this.label,
    required this.color,
  });

  /// 화면 선택 상태에 사용하는 ID입니다.
  final String id;

  /// 추천 API에 전달하는 category입니다. 대응 카테고리가 없으면 null입니다.
  final String? apiCategory;
  final String label;
  final Color color;
}
