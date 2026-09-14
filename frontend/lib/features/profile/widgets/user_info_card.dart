import 'package:flutter/material.dart';

class UserInfoCard extends StatelessWidget {
  const UserInfoCard({
    super.key,
    required this.nickname,
    required this.visitedPlaceCount,
    required this.likedPlaceCount,
  });

  final String nickname;
  final int visitedPlaceCount;
  final int likedPlaceCount;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 110,
      padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 20),
      decoration: BoxDecoration(
        color: const Color(0xFF589C7E),
        borderRadius: BorderRadius.circular(15),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              nickname,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: Colors.white,
                fontFamily: 'Paperlogy',
                fontWeight: FontWeight.w500,
                fontSize: 18,
              ),
            ),
          ),

          _PlaceCount(label: '방문한 장소', count: visitedPlaceCount),

          const SizedBox(width: 20),

          _PlaceCount(label: '좋아요한 장소', count: likedPlaceCount),
        ],
      ),
    );
  }
}

class _PlaceCount extends StatelessWidget {
  const _PlaceCount({required this.label, required this.count});

  final String label;
  final int count;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          label,
          style: const TextStyle(
            color: Colors.white,
            fontFamily: 'Paperlogy',
            fontWeight: FontWeight.w500,
            fontSize: 13,
          ),
        ),
        const SizedBox(height: 5),
        Text(
          '$count',
          style: const TextStyle(
            color: Colors.white,
            fontFamily: 'Paperlogy',
            fontWeight: FontWeight.w600,
            fontSize: 24,
          ),
        ),
      ],
    );
  }
}
