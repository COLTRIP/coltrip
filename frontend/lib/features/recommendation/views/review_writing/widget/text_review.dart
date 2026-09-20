import 'package:flutter/material.dart';

class TextReview extends StatelessWidget {
  const TextReview({super.key, required this.controller});

  final TextEditingController controller;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Text(
                '한줄평',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: Colors.black,
                ),
              ),
              SizedBox(width: 8),
              _OptionalBadge(),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 112,
            child: TextField(
              controller: controller,
              maxLength: 300,
              maxLines: null,
              expands: true,
              keyboardType: TextInputType.multiline,
              decoration: InputDecoration(
                filled: true,
                fillColor: Colors.white,
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                  borderSide: const BorderSide(color: Color(0xFFD9D9D9)),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                  borderSide: const BorderSide(color: Color(0xFF589C7E)),
                ),
                contentPadding: const EdgeInsets.all(12),
                hintText: '이 장소에서의 경험을 남겨주세요.',
                hintStyle: const TextStyle(
                  fontSize: 13,
                  color: Color(0xFF9CA3AF),
                ),
                counterText: '',
              ),
            ),
          ),
          const SizedBox(height: 5),
          Align(
            alignment: Alignment.centerRight,
            child: ListenableBuilder(
              listenable: controller,
              builder: (context, _) => Text(
                '${controller.text.length}/300',
                style: const TextStyle(fontSize: 10, color: Color(0xFF8A918E)),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _OptionalBadge extends StatelessWidget {
  const _OptionalBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFF0F1F1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: const Text(
        '선택',
        style: TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.w600,
          color: Color(0xFF777E7A),
        ),
      ),
    );
  }
}
