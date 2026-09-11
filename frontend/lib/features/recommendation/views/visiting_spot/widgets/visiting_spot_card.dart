import 'package:flutter/material.dart';
import '../../../models/recommendation.dart';


class VisitingSpotCard extends StatelessWidget {
  final SpotDetail spot;

  const VisitingSpotCard({super.key, required this.spot});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 180,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.1),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Stack(
        children: [
          ClipRRect(
            borderRadius: const BorderRadius.vertical(top: Radius.circular(10)),
            child: (spot.imageUrl == null || spot.imageUrl!.isEmpty)
                ? Container(
                    width: double.infinity,
                    height: 140,
                    color: const Color(0xFFE5E5E5),
                  )
                : Image.network(
                    spot.imageUrl!,
                    width: double.infinity,
                    height: 140,
                    fit: BoxFit.cover,
                  ),
          ),
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: Container(
              height: 38,
              padding: const EdgeInsets.fromLTRB(15, 5, 15, 0),
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(
                  bottom: Radius.circular(10),
                ),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.baseline,
                textBaseline: TextBaseline.alphabetic,
                children: [
                  Text(
                    spot.name,
                    style: const TextStyle(
                      fontFamily: 'Paperlogy',
                      fontSize: 20,
                      fontWeight: FontWeight.w700,
                      color: Colors.black,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      spot.address,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontFamily: 'Paperlogy',
                        fontSize: 10,
                        color: Color(0xFF7C7C7C),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
