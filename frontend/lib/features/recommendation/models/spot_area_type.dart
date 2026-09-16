enum SpotAreaType {
  point,
  area;

  static SpotAreaType? fromJson(String? value) {
    return switch (value) {
      'POINT' => SpotAreaType.point,
      'AREA' => SpotAreaType.area,
      _ => null,
    };
  }

  String get label => switch (this) {
    SpotAreaType.point => '점형',
    SpotAreaType.area => '면적형',
  };
}
