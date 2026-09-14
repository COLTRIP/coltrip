import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../models/place.dart';
import '../models/user_profile.dart';

class ProfileService {
  ProfileService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  Future<void> updateNickname({required String nickname}) async {
    try {
      await _dio.patch<void>('/api/users/me', data: {'nickname': nickname});
    } on DioException catch (error) {
      throw ApiException.fromDioException(error);
    }
  }

  Future<UserProfile> getMe() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>('/api/users/me');

      final data = response.data;

      if (data == null) {
        throw const ApiException('사용자 정보를 불러오지 못했습니다.');
      }

      return UserProfile.fromJson(data);
    } on DioException catch (error) {
      throw ApiException.fromDioException(error);
    }
  }

  Future<List<Place>> getLikedPlaces() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/users/me/likes',
      );

      final spots = response.data?['spots'];

      if (spots is! List) {
        return [];
      }

      return spots
          .whereType<Map<String, dynamic>>()
          .map(Place.fromSpotJson)
          .toList();
    } on DioException catch (error) {
      throw ApiException.fromDioException(error);
    }
  }

  Future<List<Place>> getVisitedPlaces() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/visits/history',
      );

      final visits = response.data?['visits'];

      if (visits is! List) {
        return [];
      }

      return visits
          .whereType<Map<String, dynamic>>()
          .map(Place.fromVisitHistoryJson)
          .toList();
    } on DioException catch (error) {
      throw ApiException.fromDioException(error);
    }
  }
}
