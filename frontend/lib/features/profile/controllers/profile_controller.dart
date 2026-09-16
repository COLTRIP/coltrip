import 'dart:developer' as developer;

import 'package:get/get.dart';

import '../../../core/network/api_exception.dart';
import '../services/profile_service.dart';
import '../models/place.dart';

/// 프로필 화면에서 사용하는 데이터와 상태를 관리하는 컨트롤러입니다.
///
/// 사용자 프로필, 좋아요한 장소, 방문한 장소를 조회하고 각 데이터의 로딩 및 오류 상태를 관리합니다.
class ProfileController extends GetxController {
  ProfileController({ProfileService? profileService})
    : _profileService = profileService ?? ProfileService();

  final ProfileService _profileService;

  final nickname = ''.obs;
  final visitedPlaceCount = 0.obs;
  final likedPlaceCount = 0.obs;
  final likedPlaces = <Place>[].obs;
  final visitedPlaces = <Place>[].obs;
  final isLikedPlacesLoading = false.obs;
  final isVisitedPlacesLoading = false.obs;

  final isLoading = false.obs;
  final profileError = RxnString();
  final likedPlacesError = RxnString();
  final visitedPlacesError = RxnString();

  @override
  void onInit() {
    super.onInit();
    loadAll();
  }

  Future<void> loadAll() async {
    await Future.wait([loadProfile(), loadLikedPlaces(), loadVisitedPlaces()]);
  }

  Future<void> loadProfile() async {
    if (isLoading.value) {
      return;
    }

    try {
      isLoading.value = true;
      profileError.value = null;

      final data = await _profileService.getMe();

      nickname.value = data.nickname;
      visitedPlaceCount.value = data.visitCount;
      likedPlaceCount.value = data.likeCount;

      developer.log(
        '프로필 조회 완료'
        '\nnickname=${nickname.value}'
        '\nvisitCount=${visitedPlaceCount.value}'
        '\nlikeCount=${likedPlaceCount.value}',
        name: 'ProfileController',
      );
    } on ApiException catch (error) {
      developer.log(
        '프로필 조회 실패'
        '\nstatusCode=${error.statusCode}'
        '\ncode=${error.code}',
        name: 'ProfileController',
      );

      profileError.value = error.message;
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
      likedPlacesError.value = null;

      final result = await _profileService.getLikedPlaces();

      likedPlaces.assignAll(result);

      developer.log(
        '좋아요 장소 조회 완료: ${result.length}개',
        name: 'ProfileController',
      );
    } on ApiException catch (error) {
      developer.log(
        '좋아요 장소 조회 실패'
        '\nstatusCode=${error.statusCode}'
        '\ncode=${error.code}',
        name: 'ProfileController',
      );

      likedPlacesError.value = error.message;
    } finally {
      isLikedPlacesLoading.value = false;
    }
  }

  Future<void> loadVisitedPlaces() async {
    if (isVisitedPlacesLoading.value) {
      return;
    }

    try {
      isVisitedPlacesLoading.value = true;
      visitedPlacesError.value = null;

      final result = await _profileService.getVisitedPlaces();
      visitedPlaces.assignAll(result);

      developer.log(
        '방문 장소 조회 완료: ${result.length}개',
        name: 'ProfileController',
      );
    } on ApiException catch (error) {
      developer.log(
        '방문 장소 조회 실패'
        '\nstatusCode=${error.statusCode}'
        '\ncode=${error.code}',
        name: 'ProfileController',
      );

      visitedPlacesError.value = error.message;
    } finally {
      isVisitedPlacesLoading.value = false;
    }
  }
}
