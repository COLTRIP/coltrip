import 'package:flutter/material.dart';

class SatisfactionSection extends StatelessWidget {
  const SatisfactionSection({
    super.key,
    required this.selected,
    required this.onSelected,
  });

  final int? selected;
  final ValueChanged<int> onSelected;

  static const _primary = Color(0xFF589C7E);
  static const _idle = Color(0xFFD5DBD8);

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
                '만족도',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.black,
                ),
              ),
              SizedBox(width: 8),
              _RequiredBadge(),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(14, 14, 14, 10),
            decoration: BoxDecoration(
              color: const Color(0xFFF7F9F8),
              border: Border.all(
                  color: const Color(0xFFE0E5E2),
              ),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: List.generate(5, (index) {
                    final score = index + 1;
                    final isActive = selected != null && score <= selected!;

                    return Semantics(
                      button: true,
                      label: '만족도 $score점',
                      selected: selected == score,
                      child: InkResponse(
                        onTap: () => onSelected(score),
                        radius: 25,
                        child: Padding(
                          padding: const EdgeInsets.all(4),
                          child: Icon(
                            isActive
                                ? Icons.star_rounded
                                : Icons.star_outline_rounded,
                            size: 36,
                            color: isActive ? _primary : _idle,
                          ),
                        ),
                      ),
                    );
                  }),
                ),
                const SizedBox(height: 4),
                Text(
                  selected == null ? '별점을 선택해주세요' : '$selected점을 선택했어요',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: selected == null
                        ? const Color(0xFF8A918E)
                        : _primary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _RequiredBadge extends StatelessWidget {
  const _RequiredBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
          horizontal: 8,
          vertical: 3
      ),
      decoration: BoxDecoration(
        color: const Color(0xFFE7F2ED),
        borderRadius: BorderRadius.circular(12),
      ),
      child: const Text(
        '필수',
        style: TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.w600,
          color: Color(0xFF39765D),
        ),
      ),
    );
  }
}
