import 'package:flutter/material.dart';

class QuietScoreGauge extends StatelessWidget {
  final int quietScore;

  const QuietScoreGauge({super.key, required this.quietScore});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 140,
      height: 140,
      child: Stack(
        alignment: Alignment.center,
        children: [
          SizedBox(
            width: 140,
            height: 140,
            child: CircularProgressIndicator(
              value: quietScore.clamp(0, 100) / 100,
              strokeWidth: 5,
              backgroundColor: const Color(0xFFE5E5E5),
              valueColor: const AlwaysStoppedAnimation(Color(0xFF589C7E)),
            ),
          ),
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('고요 지수', style: TextStyle(fontFamily: 'Paperlogy', fontSize: 10, fontWeight: FontWeight.w600, color: Colors.black)),
              const SizedBox(height: 4),
              Text(
                '$quietScore',
                style: const TextStyle(fontFamily: 'Paperlogy', fontSize: 40, fontWeight: FontWeight.w800, color: Color(0xFF589C7E)),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
