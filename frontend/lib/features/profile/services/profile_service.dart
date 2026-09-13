import 'dart:developer' as developer;

import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/storage/token_storage.dart';
import '../models/place.dart';

class ProfileService {
  ProfileService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  Future<void> updateNickname({required String nickname}) async {
    const tokenStorage = TokenStorage();

    final accessToken = await tokenStorage.readAccessToken();

    developer.log(
      '닉네임 변경 Access Token 존재 여부='
      '${accessToken != null && accessToken.isNotEmpty}',
      name: 'ProfileService',
    );

    if (accessToken == null || accessToken.isEmpty) {
      throw const ProfileException('로그인 정보가 없습니다. 다시 로그인해주세요.');
    }

    await _dio.patch<Map<String, dynamic>>(
      '/api/users/me',
      data: {'nickname': nickname},
      options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
    );
  }

  Future<Map<String, dynamic>> getMe() async {
    final response = await _dio.get<Map<String, dynamic>>('/api/users/me');

    final data = response.data;

    if (data == null) {
      throw const ProfileException('사용자 정보를 불러오지 못했습니다.');
    }

    return data;
  }

  Future<List<Place>> getLikedPlaces() async {
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
  }

  Future<List<Place>> getVisitedPlaces() async {
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
  }
}

class ProfileException implements Exception {
  const ProfileException(this.message);

  final String message;

  @override
  String toString() => message;
}
