import 'dart:developer' as developer;

import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/storage/api_environment_storage.dart';
import '../../../core/storage/token_storage.dart';
import '../models/auth_response.dart';
import '../models/auth_intent.dart';

/// Google 계정 기반의 사용자 인증을 처리하는 서비스입니다.
///
/// Google 로그인 및 회원가입을 통해 인증 정보를 서버에 전달하고, 인증 토큰 저장, 로그아웃 및 회원 탈퇴를 처리합니다.
class GoogleAuthService {
  GoogleAuthService({
    Dio? dio,
    GoogleSignIn? googleSignIn,
    TokenStorage? tokenStorage,
    ApiEnvironmentStorage? environmentStorage,
  }) : _dio = dio ?? DioClient.instance,
       _tokenStorage = tokenStorage ?? const TokenStorage(),
       _environmentStorage =
           environmentStorage ?? const ApiEnvironmentStorage(),
       _googleSignIn =
           googleSignIn ??
           GoogleSignIn(
             scopes: const ['email'],
             serverClientId:
                 '888142954996-ka5lothh80985ki57tfldiq0if0c9gmq.apps.googleusercontent.com',
           );

  final Dio _dio;
  final GoogleSignIn _googleSignIn;
  final TokenStorage _tokenStorage;
  final ApiEnvironmentStorage _environmentStorage;

  Future<AuthResponse> authenticate({required AuthIntent intent}) async {
    const logName = 'GoogleAuthService';

    developer.log('STEP 1: Google 계정 인증 시작', name: logName);

    try {
      // google_sign_in 6.x가 직전에 선택한 계정을 재사용하지 않도록
      // Google SDK 세션만 해제한 뒤 계정 선택 창을 연다.
      await _googleSignIn.signOut();
      final account = await _googleSignIn.signIn();

      if (account == null) {
        throw const AuthException('Google 로그인이 취소되었습니다.');
      }

      developer.log('STEP 2: Google 계정 인증 성공', name: logName);

      final authentication = await account.authentication;
      final idToken = authentication.idToken;

      developer.log(
        'STEP 3: ID Token 확인'
        '\n존재 여부=${idToken != null}'
        '\n길이=${idToken?.length ?? 0}',
        name: logName,
      );

      if (idToken == null) {
        throw const AuthException('Google ID Token을 받지 못했습니다.');
      }

      developer.log(
        'STEP 4: 서버 로그인 요청 시작'
        '\npath=/api/auth/google'
        '\nbaseUrl=${_dio.options.baseUrl}',
        name: logName,
      );

      final response = await _dio.post<Map<String, dynamic>>(
        '/api/auth/google',
        data: {'idToken': idToken, 'intent': intent.apiValue},
      );

      developer.log(
        'STEP 5: 서버 응답 수신'
        '\nstatusCode=${response.statusCode}'
        '\ndata 존재 여부=${response.data != null}',
        name: logName,
      );

      final data = response.data;

      if (data == null) {
        throw const AuthException('서버 응답이 비어 있습니다.');
      }

      final authResponse = AuthResponse.fromJson(data);

      await _tokenStorage.saveTokens(
        accessToken: authResponse.accessToken,
        refreshToken: authResponse.refreshToken,
      );

      developer.log(
        '토큰 저장 완료'
        '\naccessToken=${authResponse.accessToken.isNotEmpty}'
        '\nrefreshToken=${authResponse.refreshToken.isNotEmpty}',
        name: logName,
      );

      developer.log('STEP 6: Google 로그인 완료', name: logName);

      return authResponse;
    } on PlatformException catch (error, stackTrace) {
      developer.log(
        'Google 계정 인증 원본 오류'
        '\ncode=${error.code}'
        '\nmessage=${error.message}'
        '\ndetails=${error.details}'
        '\ntoString=$error',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );

      throw const AuthException('Google 계정 인증에 실패했어요. 다시 시도해주세요.');
    } on DioException catch (error, stackTrace) {
      developer.log(
        '서버 통신 실패'
        '\ntype=${error.type}'
        '\nmethod=${error.requestOptions.method}'
        '\nuri=${error.requestOptions.uri}'
        '\nstatusCode=${error.response?.statusCode}'
        '\nmessage=${error.message}',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );

      if (error.response?.statusCode == 401) {
        throw const AuthException('Google 인증 정보가 올바르지 않아요.');
      }

      throw AuthException(_extractErrorMessage(error));
    } on AuthException catch (error, stackTrace) {
      developer.log(
        '인증 처리 실패: ${error.message}',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );

      rethrow;
    } catch (error, stackTrace) {
      developer.log(
        '예상하지 못한 로그인 오류',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );

      throw const AuthException('로그인 중 알 수 없는 오류가 발생했어요.');
    } finally {
      developer.log('Google 로그인 처리 종료', name: logName);
    }
  }

  Future<void> logout() async {
    const logName = 'GoogleAuthService';

    developer.log('로그아웃 시작', name: logName);

    try {
      final accessToken = await _tokenStorage.readAccessToken();

      developer.log(
        'Access Token 존재 여부=${accessToken != null && accessToken.isNotEmpty}',
        name: logName,
      );

      if (accessToken != null && accessToken.isNotEmpty) {
        final response = await _dio.post<void>(
          '/api/auth/logout',
          options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
        );

        developer.log(
          '서버 로그아웃 완료'
          '\nstatusCode=${response.statusCode}',
          name: logName,
        );
      }
    } on DioException catch (error, stackTrace) {
      developer.log(
        '서버 로그아웃 실패, 로컬 로그아웃 계속 진행'
        '\nstatusCode=${error.response?.statusCode}'
        '\ntype=${error.type}',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );
    } finally {
      await _tokenStorage.clearTokens();
      await _environmentStorage.setDemoMode(false);
      DioClient.configureEnvironment(demoMode: false);

      try {
        await _googleSignIn.signOut();
        developer.log('로컬 및 Google 로그아웃 완료', name: logName);
      } catch (error, stackTrace) {
        developer.log(
          'Google 로그아웃 실패',
          name: logName,
          error: error,
          stackTrace: stackTrace,
        );
      }
    }
  }

  Future<void> deleteAccount() async {
    const logName = 'GoogleAuthService';

    developer.log('회원 탈퇴 시작', name: logName);

    try {
      final accessToken = await _tokenStorage.readAccessToken();

      if (accessToken == null || accessToken.isEmpty) {
        throw const AuthException('로그인 정보가 없습니다. 다시 로그인해주세요.');
      }

      final response = await _dio.delete<dynamic>(
        '/api/users/me',
        options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
      );

      developer.log(
        '회원 탈퇴 API 완료'
        '\nstatusCode=${response.statusCode}',
        name: logName,
      );

      // 서버에서 회원 탈퇴가 성공한 뒤에만 로컬 정보 삭제
      await _tokenStorage.clearTokens();
      await _googleSignIn.signOut();

      developer.log('회원 탈퇴 및 로컬 정보 삭제 완료', name: logName);
    } on DioException catch (error, stackTrace) {
      developer.log(
        '회원 탈퇴 API 실패'
        '\nstatusCode=${error.response?.statusCode}',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );

      throw AuthException(_extractDeleteAccountErrorMessage(error));
    } on AuthException {
      rethrow;
    } catch (error, stackTrace) {
      developer.log(
        '회원 탈퇴 처리 실패',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );

      throw const AuthException('회원 탈퇴 중 오류가 발생했어요.');
    }
  }
}

/// 인증 처리 중 발생한 오류를 나타내는 예외입니다.
///
/// 사용자에게 전달할 인증 관련 오류 메시지를 포함합니다.
class AuthException implements Exception {
  const AuthException(this.message);

  final String message;

  @override
  String toString() => message;
}

String _extractErrorMessage(DioException error) {
  return _extractNetworkErrorMessage(error, fallback: '로그인 요청에 실패했어요.');
}

String _extractDeleteAccountErrorMessage(DioException error) {
  final statusCode = error.response?.statusCode;
  if (statusCode == 401) {
    return '로그인 세션이 만료되었습니다. 다시 로그인해주세요.';
  }

  if (statusCode == 403) {
    return '회원 탈퇴 권한이 없습니다.';
  }

  if (statusCode != null && statusCode >= 500) {
    return '서버에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.';
  }

  return _extractNetworkErrorMessage(error, fallback: '회원 탈퇴 요청에 실패했어요.');
}

String _extractNetworkErrorMessage(
  DioException error, {
  required String fallback,
}) {
  final data = error.response?.data;
  final serverMessage = data is Map ? data['message']?.toString() : null;

  if (serverMessage != null && serverMessage.isNotEmpty) {
    return serverMessage;
  }

  return switch (error.type) {
    DioExceptionType.connectionTimeout => '서버 연결 시간이 초과됐어요.',
    DioExceptionType.sendTimeout => '요청 전송 시간이 초과됐어요.',
    DioExceptionType.receiveTimeout => '서버 응답 시간이 초과됐어요.',
    DioExceptionType.connectionError => '서버에 연결할 수 없어요.',
    DioExceptionType.cancel => '요청이 취소됐어요.',
    _ => fallback,
  };
}
