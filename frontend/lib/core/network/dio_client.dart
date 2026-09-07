import 'package:dio/dio.dart';

class DioClient {
  DioClient._();

  static final Dio instance = Dio(
    BaseOptions(
      baseUrl: 'https://yeast-alphabetical-sandwich-vbulletin.trycloudflare.com',

      connectTimeout: const Duration(seconds: 10),
      sendTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),

      contentType: Headers.jsonContentType,
      responseType: ResponseType.json,
    ),
  );
}
