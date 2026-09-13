import 'dart:developer' as developer;

import 'package:dio/dio.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/network/token_storage.dart';
import '../models/auth_response.dart';
import '../models/auth_intent.dart';

class GoogleAuthService {
  GoogleAuthService({Dio? dio}) : _dio = dio ?? DioClient.instance;

  final Dio _dio;

  Future<AuthResponse> authenticate({required AuthIntent intent}) async {
    const logName = 'GoogleAuthService';

    developer.log('STEP 1: Google 계정 인증 시작', name: logName);

    try {
      final account = await GoogleSignIn.instance.authenticate();

      developer.log(
        'STEP 2: Google 계정 인증 성공'
        '\nemail=${account.email}'
        '\naccountId=${account.id}',
        name: logName,
      );

      final idToken = account.authentication.idToken;

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

      const tokenStorage = TokenStorage();

      await tokenStorage.saveTokens(
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
    } on GoogleSignInException catch (error, stackTrace) {
      developer.log(
        'Google 계정 인증 실패'
        '\ncode=${error.code}'
        '\ndescription=${error.description}',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );

      throw AuthException(error.description ?? 'Google 계정 인증에 실패했습니다.');
    } on DioException catch (error, stackTrace) {
      developer.log(
        '서버 통신 실패'
        '\ntype=${error.type}'
        '\nmethod=${error.requestOptions.method}'
        '\nuri=${error.requestOptions.uri}'
        '\nstatusCode=${error.response?.statusCode}'
        '\nresponse=${error.response?.data}'
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
    const tokenStorage = TokenStorage();

    developer.log('로그아웃 시작', name: logName);

    try {
      final accessToken = await tokenStorage.readAccessToken();

      developer.log(
        'Access Token 존재 여부=${accessToken != null && accessToken.isNotEmpty}',
        name: logName,
      );

      if (accessToken == null || accessToken.isEmpty) {
        throw const AuthException('로그인 정보가 없습니다. 다시 로그인해주세요.');
      }

      final response = await _dio.post<void>(
        '/api/auth/logout',
        options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
      );

      developer.log(
        '서버 로그아웃 완료'
        '\nstatusCode=${response.statusCode}',
        name: logName,
      );
    } on DioException catch (error, stackTrace) {
      developer.log(
        '서버 로그아웃 실패'
        '\nstatusCode=${error.response?.statusCode}'
        '\nresponse=${error.response?.data}',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );

      throw AuthException(_extractLogoutErrorMessage(error));
    } on AuthException {
      rethrow;
    } catch (error, stackTrace) {
      developer.log(
        '로그아웃 처리 실패',
        name: logName,
        error: error,
        stackTrace: stackTrace,
      );

      throw const AuthException('로그아웃 중 오류가 발생했어요.');
    } finally {
      await tokenStorage.clearTokens();

      try {
        await GoogleSignIn.instance.signOut();
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
    const tokenStorage = TokenStorage();

    developer.log('회원 탈퇴 시작', name: logName);

    try {
      final accessToken = await tokenStorage.readAccessToken();

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
      await tokenStorage.clearTokens();
      await GoogleSignIn.instance.signOut();

      developer.log('회원 탈퇴 및 로컬 정보 삭제 완료', name: logName);
    } on DioException catch (error, stackTrace) {
      developer.log(
        '회원 탈퇴 API 실패'
        '\nstatusCode=${error.response?.statusCode}'
        '\nresponse=${error.response?.data}',
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

class AuthException implements Exception {
  const AuthException(this.message);

  final String message;

  @override
  String toString() => message;
}

String _extractErrorMessage(DioException error) {
  final data = error.response?.data;

  if (data is Map) {
    final message = data['message'];

    if (message is String && message.isNotEmpty) {
      return message;
    }
  }

  return switch (error.type) {
    DioExceptionType.connectionTimeout => '서버 연결 시간이 초과됐어요.',
    DioExceptionType.sendTimeout => '요청 전송 시간이 초과됐어요.',
    DioExceptionType.receiveTimeout => '서버 응답 시간이 초과됐어요.',
    DioExceptionType.connectionError => '서버에 연결할 수 없어요.',
    DioExceptionType.badResponse => '로그인 요청에 실패했어요.',
    DioExceptionType.cancel => '요청이 취소됐어요.',
    _ => '로그인 중 오류가 발생했어요.',
  };
}

String _extractLogoutErrorMessage(DioException error) {
  final data = error.response?.data;

  if (error.response?.statusCode == 401) {
    return '로그인 세션이 만료되었습니다.';
  }

  if (data is Map) {
    final message = data['message'];

    if (message is String && message.isNotEmpty) {
      return message;
    }
  }

  return switch (error.type) {
    DioExceptionType.connectionTimeout => '서버 연결 시간이 초과됐어요.',
    DioExceptionType.sendTimeout => '요청 전송 시간이 초과됐어요.',
    DioExceptionType.receiveTimeout => '서버 응답 시간이 초과됐어요.',
    DioExceptionType.connectionError => '서버에 연결할 수 없어요.',
    DioExceptionType.cancel => '로그아웃 요청이 취소됐어요.',
    _ => '로그아웃 요청에 실패했어요.',
  };
}

String _extractDeleteAccountErrorMessage(DioException error) {
  final statusCode = error.response?.statusCode;
  final data = error.response?.data;

  if (statusCode == 401) {
    return '로그인 세션이 만료되었습니다. 다시 로그인해주세요.';
  }

  if (statusCode == 403) {
    return '회원 탈퇴 권한이 없습니다.';
  }

  if (statusCode != null && statusCode >= 500) {
    return '서버에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.';
  }

  if (data is Map) {
    final message = data['message'];

    if (message is String && message.isNotEmpty) {
      return message;
    }
  }

  return switch (error.type) {
    DioExceptionType.connectionTimeout => '서버 연결 시간이 초과됐어요.',
    DioExceptionType.sendTimeout => '요청 전송 시간이 초과됐어요.',
    DioExceptionType.receiveTimeout => '서버 응답 시간이 초과됐어요.',
    DioExceptionType.connectionError => '서버에 연결할 수 없어요.',
    DioExceptionType.cancel => '회원 탈퇴 요청이 취소됐어요.',
    _ => '회원 탈퇴 요청에 실패했어요.',
  };
}
