import 'dart:developer' as developer;

import 'package:dio/dio.dart';
import 'package:get/get.dart';

import '../services/profile_service.dart';
import '../models/place.dart';

class ProfileController extends GetxController {
  ProfileController({ProfileService? profileService})
    : _profileService = profileService ?? ProfileService();

  final ProfileService _profileService;

  final nickname = ''.obs;
  final visitedPlaceCount = 0.obs;
  final likedPlaceCount = 0.obs;
  final likedPlaces = <Place>[].obs;
  final isLikedPlacesLoading = false.obs;

  final isLoading = false.obs;
  final errorMessage = RxnString();

  Future<void> loadProfile() async {
    if (isLoading.value) {
      return;
    }

    try {
      isLoading.value = true;
      errorMessage.value = null;

      final data = await _profileService.getMe();

      nickname.value = data['nickname'] as String? ?? '';

      visitedPlaceCount.value = data['visitCount'] as int? ?? 0;

      likedPlaceCount.value = data['likeCount'] as int? ?? 0;

      developer.log(
        '프로필 조회 완료'
        '\nnickname=${nickname.value}'
        '\nvisitCount=${visitedPlaceCount.value}'
        '\nlikeCount=${likedPlaceCount.value}',
        name: 'ProfileController',
      );
    } on DioException catch (error) {
      developer.log(
        '프로필 조회 실패'
        '\nstatusCode=${error.response?.statusCode}'
        '\nresponse=${error.response?.data}',
        name: 'ProfileController',
      );

      errorMessage.value = error.response?.data is Map
          ? error.response?.data['message']
          : '프로필을 불러오지 못했습니다.';
    } finally {
      isLoading.value = false;
    }
  }

  Future<void> loadLikedPlaces() async {
    if (isLikedPlacesLoading.value) {
      return;
    }

    try {
      isLikedPlacesLoading.value = true;
      errorMessage.value = null;

      final result = await _profileService.getLikedPlaces();

      likedPlaces.assignAll(result);

      developer.log(
        '좋아요 장소 조회 완료: ${result.length}개',
        name: 'ProfileController',
      );
    } on DioException catch (error) {
      developer.log(
        '좋아요 장소 조회 실패'
        '\nstatusCode=${error.response?.statusCode}'
        '\nresponse=${error.response?.data}',
        name: 'ProfileController',
      );

      errorMessage.value = error.response?.data is Map
          ? error.response?.data['message'] as String?
          : '좋아요 장소를 불러오지 못했습니다.';
    } finally {
      isLikedPlacesLoading.value = false;
    }
  }
}
