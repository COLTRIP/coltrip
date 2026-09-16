import 'package:flutter/material.dart';

import '../models/place_type_item.dart';

abstract final class PlaceTypeData {
  static const items = <PlaceTypeItem>[
    PlaceTypeItem(
        id: 'CAFE',
        label: '☕ 카페',
        color: Color(0xFFF2C38B),
    ),
    PlaceTypeItem(
        id: 'NATURE',
        label: '🌲 자연 · 공원',
        color: Color(0xFFA8DDB8),
    ),
    PlaceTypeItem(
        id: 'CULTURE',
        label: '🖼️ 문화시설',
        color: Color(0xFFC5B5EA),
    ),
    PlaceTypeItem(
      id: 'ALLEY',
      label: '🏙️ 도심 · 랜드마크',
      color: Color(0xFFAFCAD8),
    ),
    PlaceTypeItem(
        id: 'TEMPLE',
        label: '⛩ 역사 · 종교',
        color: Color(0xFFE7C77F),
    ),
    PlaceTypeItem(
      id: 'EXPERIENCE',
      label: '️🧩 체험 · 액티비티',
      color: Color(0xFFF3AD9D),
    ),
    PlaceTypeItem(
        id: 'BOOK',
        label: '📚 도서관 · 서점',
        color: Color(0xFFAFC4EB),
    ),
    PlaceTypeItem(
        id: 'BEACH',
        label: '️🌊 해변',
        color: Color(0xFF91D6E3),
    ),
  ];
}
