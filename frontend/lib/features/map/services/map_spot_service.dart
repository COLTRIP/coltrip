import '../../../core/network/dio_client.dart';
import '../models/map_spot.dart';

class MapSpotService {
  const MapSpotService();

  Future<List<MapSpot>> findInBounds({
    required double swLat,
    required double swLng,
    required double neLat,
    required double neLng,
    String? category,
    String? mode,
  }) async {
    final response = await DioClient.instance.get<Map<String, dynamic>>(
      '/api/spots',
      queryParameters: {
        'swLat': swLat,
        'swLng': swLng,
        'neLat': neLat,
        'neLng': neLng,
        if (category != null) 'category': category,
        if (mode != null) 'mode': mode,
      },
    );

    final items = response.data?['spots'];
    if (items is! List) return const [];

    return items
        .whereType<Map<String, dynamic>>()
        .map(MapSpot.fromJson)
        .toList();
  }
}
