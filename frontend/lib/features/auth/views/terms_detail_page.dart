import 'package:flutter/material.dart';

/// 약관 항목 옆 화살표를 눌렀을 때 여는 전문 화면.
class TermsDetailPage extends StatelessWidget {
  final String title;
  final String content;

  const TermsDetailPage({
    super.key,
    required this.title,
    required this.content,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        foregroundColor: Colors.black,
        title: Text(
          title,
          style: const TextStyle(
            fontFamily: 'Paperlogy',
            fontWeight: FontWeight.w600,
            fontSize: 16,
            color: Color(0xFF252B28),
          ),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Text(
            content,
            style: const TextStyle(
              fontFamily: 'Paperlogy',
              fontWeight: FontWeight.w400,
              fontSize: 13,
              height: 1.7,
              color: Color(0xFF474444),
            ),
          ),
        ),
      ),
    );
  }
}
