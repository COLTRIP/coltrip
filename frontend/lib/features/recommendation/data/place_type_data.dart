import 'package:flutter/material.dart';

import '../models/place_type_item.dart';

abstract final class PlaceTypeData {
  static const items = <PlaceTypeItem>[
    PlaceTypeItem(
      id: 'CAFE',
      apiCategory: 'CAFE',
      label: '☕ 카페',
      color: Color(0xFFF2C38B),
    ),
    PlaceTypeItem(
      id: 'NATURE',
      apiCategory: 'PARK',
      label: '🌲 자연 · 공원',
      color: Color(0xFFA8DDB8),
    ),
    PlaceTypeItem(
      id: 'CULTURE',
      apiCategory: 'GALLERY',
      label: '🖼️ 문화시설',
      color: Color(0xFFC5B5EA),
    ),
    PlaceTypeItem(
      id: 'ALLEY',
      apiCategory: 'ALLEY',
      label: '🏙️ 도심 · 랜드마크',
      color: Color(0xFFAFCAD8),
    ),
    PlaceTypeItem(
      id: 'TEMPLE',
      apiCategory: 'TEMPLE',
      label: '⛩ 역사 · 종교',
      color: Color(0xFFE7C77F),
    ),
    PlaceTypeItem(
      id: 'EXPERIENCE',
      apiCategory: null,
      label: '️🧩 체험 · 액티비티',
      color: Color(0xFFF3AD9D),
    ),
    PlaceTypeItem(
      id: 'BOOK',
      apiCategory: 'LIBRARY',
      label: '📚 도서관 · 서점',
      color: Color(0xFFAFC4EB),
    ),
    PlaceTypeItem(
      id: 'BEACH',
      apiCategory: 'BEACH',
      label: '️🌊 해변',
      color: Color(0xFF91D6E3),
    ),
  ];
}
