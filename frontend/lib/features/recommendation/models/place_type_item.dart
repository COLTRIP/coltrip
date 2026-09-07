import 'package:flutter/material.dart';


class PlaceTypeItem {
  const PlaceTypeItem({
    required this.id,
    required this.label,
    required this.color,
  });

  final String id;
  final String label;
  final Color color;
}