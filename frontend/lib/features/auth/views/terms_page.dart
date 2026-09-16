import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown_plus/flutter_markdown_plus.dart';

class TermsPage extends StatefulWidget {
  const TermsPage({super.key});

  static const String rawUrl =
      'https://raw.githubusercontent.com/COLTRIP/coltrip/develop/docs/privacy-policy.md';

  @override
  State<TermsPage> createState() => _TermsPageState();
}

class _TermsPageState extends State<TermsPage> {
  final Dio _dio = Dio();

  String? _markdownText;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _fetchTerms();
  }

  Future<void> _fetchTerms() async {
    try {
      final response = await _dio.get<String>(TermsPage.rawUrl);

      setState(() {
        _markdownText = response.data ?? '';
      });
    } catch (error) {
      setState(() {
        _errorMessage = '약관을 불러오지 못했어요. 잠시 후 다시 시도해주세요.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('이용약관')),
      body: SafeArea(
        child: Builder(
          builder: (context) {
            if (_errorMessage != null) {
              return Center(child: Text(_errorMessage!));
            }

            if (_markdownText == null) {
              return const Center(child: CircularProgressIndicator());
            }

            return Markdown(data: _markdownText!);
          },
        ),
      ),
    );
  }
}
