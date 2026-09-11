import 'package:flutter/material.dart';


class PlaceTypeItem {
  const PlaceTypeItem({
    required this.id,
    required this.label,
    required this.color,
    required this.category,
  });

  final String id;
  final String label;
  final Color color;

  /// 백엔드 category enum 값 (예: 'CAFE'). GET /api/spots?category= 에 그대로 씀.
  final String category;
}