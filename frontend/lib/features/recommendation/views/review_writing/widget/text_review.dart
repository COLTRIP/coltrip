import 'package:flutter/material.dart';

class TextReview extends StatelessWidget {
  const TextReview({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Align(
            alignment: AlignmentGeometry.centerLeft,
            child: Text(
              '리뷰 작성',
              style: TextStyle(
                fontFamily: 'Paperlogy',
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: Colors.black,
              ),
            ),
          ),
          const SizedBox(height: 16),
          Container(
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border.all(color: const Color(0xFFD9D9D9), width: 1, ),
              borderRadius: BorderRadius.circular(10)
            ),
            height: 200,
            child: const TextField(
              maxLines: null,
              expands: true,
              keyboardType: TextInputType.multiline,
              decoration: InputDecoration(
                border: InputBorder.none,
                contentPadding: EdgeInsets.all(12),
              ),
            ),
          ),

        ],
      ),
    );
  }
}
