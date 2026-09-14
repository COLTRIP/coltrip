import 'package:flutter/material.dart';

import '../../../models/quiet_score_point.dart';

class QuietScoreTimelineChart extends StatelessWidget {
  final List<QuietScorePoint> points;

  const QuietScoreTimelineChart({super.key, required this.points});

  Color _barColor(int score) {
    if (score >= 70) return const Color(0xFF589C7E);
    if (score >= 40) return const Color(0xFFD0F094);
    return const Color(0xFFFFD483);
  }

  @override
  Widget build(BuildContext context) {
    if (points.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(color: const Color(0xFFD9D9D9)),
          borderRadius: BorderRadius.circular(10),
        ),
        child: const Center(
          child: Text('타임라인 데이터가 없어요.', style: TextStyle(fontFamily: 'Paperlogy', fontSize: 12, color: Color(0xFF7C7C7C))),
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
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              height: 120,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: points.map((point) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 2),
                    child: Container(
                      width: 16,
                      height: 120 * (point.score.clamp(0, 100) / 100),
                      decoration: BoxDecoration(
                        color: _barColor(point.score),
                        borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
            const Divider(height: 10, color: Color(0x33252B28)),
            Row(
              children: points.map((point) {
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 2),
                  child: SizedBox(
                    width: 16,
                    child: Text(
                      point.hour.toString().padLeft(2, '0'),
                      textAlign: TextAlign.center,
                      style: const TextStyle(fontFamily: 'Paperlogy', fontSize: 10, color: Colors.black),
                    ),
                  ),
                );
              }).toList(),
            ),
          ],
        ),
      ),
    );
  }
}
