import 'package:dio/dio.dart';


class DioClient {
  DioClient._();

  static final Dio instance = Dio(
    BaseOptions(
      baseUrl: 'https://organ-aimed-saying-revised.trycloudflare.com',

      connectTimeout: const Duration(seconds: 10),
      sendTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),

      contentType: Headers.jsonContentType,
      responseType: ResponseType.json,
    ),
  );
}