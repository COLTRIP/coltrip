import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

class SatisfactionSection extends StatelessWidget {
  const SatisfactionSection({
    super.key,
    required this.selected,
    required this.onSelected,
  });

  final int? selected;
  final ValueChanged<int> onSelected;

  static const _primary = Color(0xFF589C7E);
  static const _idleBg = Color(0xFFE5E5E5);
  static const _iconColor = Color(0xFF111827);
  static const _labelColor = Color(0xFF6B7280);

  static const _steps = <_ScaleStep>[
    _ScaleStep(score: 1, asset: 'assets/icons/face_angry.svg'),
    _ScaleStep(score: 2, asset: 'assets/icons/face_frown.svg'),
    _ScaleStep(score: 3, asset: 'assets/icons/face_meh.svg'),
    _ScaleStep(score: 4, asset: 'assets/icons/face_smile.svg'),
    _ScaleStep(score: 5, asset: 'assets/icons/face_star.svg'),
  ];

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '만족도',
            style: TextStyle(
              fontFamily: 'Paperlogy',
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: Colors.black,
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: _steps.map((step) {
              final isSelected = selected == step.score;
              return Column(
                children: [
                  GestureDetector(
                    onTap: () => onSelected(step.score),
                    child: Container(
                      width: 36,
                      height: 36,
                      decoration: BoxDecoration(
                        color: isSelected ? _primary : _idleBg,
                        shape: BoxShape.circle,
                      ),
                      alignment: Alignment.center,
                      child: SvgPicture.asset(
                        step.asset,
                        width: 18,
                        height: 18,
                        colorFilter: ColorFilter.mode(
                          isSelected ? Colors.white : _iconColor,
                          BlendMode.srcIn,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${step.score}',
                    style: const TextStyle(
                      fontFamily: 'Paperlogy',
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: _labelColor,
                    ),
                  ),
                ],
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}

class _ScaleStep {
  final int score;
  final String asset;

  const _ScaleStep({required this.score, required this.asset});
}
