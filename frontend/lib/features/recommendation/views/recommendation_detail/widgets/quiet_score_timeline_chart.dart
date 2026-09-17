import 'package:flutter/material.dart';

import '../../../models/quiet_score_point.dart';

class QuietScoreTimelineChart extends StatelessWidget {
  final List<QuietScorePoint> points;

  const QuietScoreTimelineChart({super.key, required this.points});

  static const _weekdays = ['월', '화', '수', '목', '금', '토', '일'];

  Color _barColor(int score) {
    if (score >= 70) return const Color(0xFF589C7E);
    if (score >= 40) return const Color(0xFFE4A94B);
    return const Color(0xFFC96363);
  }

  @override
  Widget build(BuildContext context) {
    final hasTimelineData = points.any((point) => point.score != null);

    if (!hasTimelineData) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 28),
        decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(color: const Color(0xFFD9D9D9)),
          borderRadius: BorderRadius.circular(10),
        ),
        child: const Center(
          child: Text(
            '해당 장소의 타임라인 정보가 제공되지 않아요.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 13, color: Color(0xFF7C7C7C)),
          ),
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: const Color(0xFFD9D9D9)),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        children: [
          SizedBox(
            height: 132,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: points.map((point) {
                final score = point.score;
                final barHeight = score == null
                    ? 4.0
                    : 94 * (score.clamp(0, 100) / 100);

                return Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Text(
                        score?.toString() ?? '-',
                        style: const TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.w500,
                          color: Color(0xFF59605D),
                        ),
                      ),
                      const SizedBox(height: 5),
                      Container(
                        width: 22,
                        height: barHeight,
                        decoration: BoxDecoration(
                          color: score == null
                              ? const Color(0xFFE5E8E6)
                              : _barColor(score),
                          borderRadius: const BorderRadius.vertical(
                            top: Radius.circular(5),
                          ),
                        ),
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),
          const Divider(height: 10, color: Color(0x33252B28)),
          Row(
            children: points.map((point) {
              return Expanded(
                child: Column(
                  children: [
                    Text(
                      _weekdays[point.targetAt.weekday - 1],
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFF252B28),
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '${point.targetAt.month}/${point.targetAt.day}',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 9,
                        color: Color(0xFF8A918E),
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}
