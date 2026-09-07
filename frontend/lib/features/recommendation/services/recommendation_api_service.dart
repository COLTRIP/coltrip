import 'package:dio/dio.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../models/recommendation.dart';

class RecommendationApiService {
  RecommendationApiService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  // TODO: 리스트 불러올 때 일단 임의로 부산 전체
  static const _busanSwLat = 34.98;
  static const _busanSwLng = 128.75;
  static const _busanNeLat = 35.40;
  static const _busanNeLng = 129.30;

  // TODO: 로그인 연동 전 임시. 협업자에게 받은 테스트용 accessToken을 넣을 것.
  static const _tempAccessToken = '';

  /// GET /api/spots — bounding box 안의 추천 장소 목록
  Future<List<Spot>> getSpots({String? category, String? mode}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots',
        queryParameters: {
          'swLat': _busanSwLat,
          'swLng': _busanSwLng,
          'neLat': _busanNeLat,
          'neLng': _busanNeLng,
          if (category != null && category.isNotEmpty && category != '전체')
            'category': category,
          if (mode != null && mode.isNotEmpty) 'mode': mode,
        },
        options: Options(
          headers: {'Authorization': 'Bearer $_tempAccessToken'},
        ),
      );

      final list = response.data?['spots'] as List? ?? const [];
      return list
          .cast<Map<String, dynamic>>()
          .map(Spot.fromJson)
          .toList();
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<SpotDetail> getSpotDetail({required int spotId}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/spots/$spotId',
        options: Options(
          headers: {'Authorization': 'Bearer $_tempAccessToken'},
        ),
      );
      return SpotDetail.fromJson(response.data!);
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<void> toggleLike(int spotId) async {

  }
}
