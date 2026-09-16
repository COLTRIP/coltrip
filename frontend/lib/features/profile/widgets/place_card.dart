import 'package:flutter/material.dart';

import '../models/place.dart';

/// 장소 정보를 카드 형태로 표시하는 위젯입니다.
///
/// 장소 이미지와 이름을 표시하며, 프로필 화면의 장소 캐러셀에서 사용됩니다.
class PlaceCard extends StatelessWidget {
  const PlaceCard({super.key, required this.place, this.onTap});

  final Place place;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 200,
      child: Card(
        margin: const EdgeInsets.only(bottom: 12),
        elevation: 7,
        shadowColor: Colors.black.withValues(alpha: 0.14),
        color: Colors.white,
        clipBehavior: Clip.antiAlias,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        child: InkWell(
          onTap: onTap,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 방문한 장소 캐러셀 - 장소 사진
              Expanded(
                child: SizedBox(
                  width: double.infinity,
                  child: Image.network(
                    place.imageUrl,
                    fit: BoxFit.cover,
                    errorBuilder: (_, __, ___) {
                      return const ColoredBox(
                        color: Color(0xFFE9ECEF),
                        child: Center(
                          child: Icon(
                            Icons.image_not_supported_outlined,
                            color: Colors.grey,
                            size: 40,
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ),

              // 방문한 장소 캐러셀 - 장소 이름
              Padding(
                padding: const EdgeInsets.symmetric(
                  vertical: 8,
                  horizontal: 10,
                ),
                child: Text(
                  place.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w400,
                    color: Color(0xFF252B28),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
