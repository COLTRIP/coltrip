import 'package:dio/dio.dart';

class ApiClient {
  static const String baseUrl = 'https://untwist-malformed-cause.ngrok-free.dev';

  ApiClient._internal()
      : dio = Dio(
          BaseOptions(
            baseUrl: baseUrl,
            headers: {
              'ngrok-skip-browser-warning': 'true',
            },
          ),
        );

  static final ApiClient instance = ApiClient._internal();

  final Dio dio;
}
