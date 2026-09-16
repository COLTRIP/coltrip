import '../models/place_mood_item.dart';

abstract final class PlaceMoodData {
  static const items = <PlaceMoodItem>[
    PlaceMoodItem(id: 'COZY', label: '아늑함'),
    PlaceMoodItem(id: 'NATURAL', label: '자연'),
    PlaceMoodItem(id: 'URBAN', label: '도시'),
    PlaceMoodItem(id: 'VINTAGE', label: '빈티지'),
    PlaceMoodItem(id: 'EXOTIC', label: '이국적'),
    PlaceMoodItem(id: 'VIBRANT', label: '활기찬'),
    PlaceMoodItem(id: 'SENSORY', label: '감각적'),
    PlaceMoodItem(id: 'TRANQUIL', label: '고요한'),
  ];

  static String labelFor(String id) {
    for (final mood in items) {
      if (mood.id == id) {
        return mood.label;
      }
    }

    return id;
  }
}
