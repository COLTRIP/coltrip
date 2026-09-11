import 'package:flutter/material.dart';

import '../models/place_type_item.dart';


abstract final class PlaceTypeData {
  static const items = <PlaceTypeItem>[
    PlaceTypeItem(
      id: 'cafe',
      label: '☕ 카페',
      color: Color(0xFFCDA984),
      category: 'CAFE',
    ),
    PlaceTypeItem(
      id: 'park',
      label: '🌲 공원',
      color: Color(0xFFD0F8D1),
      category: 'PARK',
    ),
    PlaceTypeItem(
      id: 'library',
      label: '📚 도서관',
      color: Color(0xFFB9F5DF),
      category: 'LIBRARY',
    ),
    PlaceTypeItem(
      id: 'exhibition',
      label: '🖼 미술관·전시',
      color: Color(0xFFF6C1EE),
      category: 'GALLERY',
    ),
    PlaceTypeItem(
      id: 'bookstore',
      label: '📖 서점',
      color: Color(0xFFFFD7B8),
      category: 'BOOKSTORE',
    ),
    PlaceTypeItem(
      id: 'temple',
      label: '⛩️사찰',
      color: Color(0xFFFFE4B0),
      category: 'TEMPLE',
    ),
    PlaceTypeItem(
      id: 'beach',
      label: '🌊 해변',
      color: Color(0xFFC8F0FA),
      category: 'BEACH',
    ),
    PlaceTypeItem(
      id: 'street',
      label: '🏘️골목·거리',
      color: Color(0xFFFFB1B3),
      category: 'ALLEY',
    ),
  ];
}