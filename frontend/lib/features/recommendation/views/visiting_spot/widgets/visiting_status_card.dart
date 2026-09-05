import 'package:flutter/material.dart';
import '../../../models/visit_status.dart';

class VisitingStatusCard extends StatelessWidget {
  final VisitStatus status;

  const VisitingStatusCard({super.key, required this.status});

  @override
  Widget build(BuildContext context) {
    return switch (status) {
      VisitStatus.visiting => const _VisitingBox(),
      VisitStatus.crowdingDetected => const _CrowdingBox(),
      VisitStatus.notAtSpot => const _Missbox(),
      VisitStatus.completed => const _CompletedBox(),
    };
  }
}


class _VisitingBox extends StatelessWidget {
  const _VisitingBox();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFC6E0D4),
        borderRadius: BorderRadius.circular(10),
      ),
      child: const Text(
        '장소에 도착하면 현재 위치를 확인해\n방문을 인증할 수 있어요.',
        style: TextStyle(
          fontWeight: FontWeight.w600,
          fontFamily: 'Paperlogy',
          fontSize: 14,
        ),
        textAlign: TextAlign.center,
      ),
    );
  }
}

class _CrowdingBox extends StatelessWidget {
  const _CrowdingBox();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFC6E0D4),
        borderRadius: BorderRadius.circular(10),
      ),
      child: const Text(
        '현재 목적지가 예상보다 혼잡해졌어요.\n 근처의 다른 콜드 플레이스를 추천해드릴까요?',
        style: TextStyle(
          fontWeight: FontWeight.w600,
          fontFamily: 'Paperlogy',
          fontSize: 14,
        ),
        textAlign: TextAlign.center,
      ),
    );
  }
}

class _CompletedBox extends StatelessWidget {
  const _CompletedBox();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFC6E0D4),
        borderRadius: BorderRadius.circular(10),
      ),
      child: const Text(
        '방문이 확인되었어요!\n조용한 시간을 즐기고, 경험을 리뷰로 남겨보세요.',
        style: TextStyle(
          fontWeight: FontWeight.w600,
          fontFamily: 'Paperlogy',
          fontSize: 14,
        ),
        textAlign: TextAlign.center,
      ),
    );
  }
}

class _Missbox extends StatelessWidget {
  const _Missbox();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 10,horizontal: 16),
      decoration: BoxDecoration(
        color: const Color(0xFFF2CECE),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Expanded(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              '아직 방문 장소에 도착하지 않았어요.\n조금 더 가까이에서 다시 확인해주세요.',
              style: TextStyle(
                fontWeight: FontWeight.w600,
                fontFamily: 'Paperlogy',
                fontSize: 16,
              ),
              textAlign: TextAlign.left,
            ),IconButton(onPressed: () {
              //TOdo: 뷰모델에서 스테이터스 바꾸기
            },icon: const Icon(Icons.close, color: Colors.black),),
          ],
        ),
      ),
    );
  }
}