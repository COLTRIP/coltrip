import 'package:flutter/material.dart';

import '../models/place.dart';
import 'place_card.dart';

class PlaceCarouselSection extends StatelessWidget {
  const PlaceCarouselSection({
    super.key,
    required this.title,
    required this.places,
    this.onMorePressed,
    this.maxVisibleItems = 5,
    this.emptyMessage = '아직 등록된 장소가 없어요.',
  });

  final String title;
  final List<Place> places;
  final VoidCallback? onMorePressed;
  final int maxVisibleItems;
  final String emptyMessage;

  @override
  Widget build(BuildContext context) {
    final visibleItemCount = places.length > maxVisibleItems
        ? maxVisibleItems
        : places.length;

    final hasMore = places.length > maxVisibleItems;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 30),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontFamily: 'Paperlogy',
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF252B28),
                ),
              ),
              if (places.isNotEmpty) ...[
                const SizedBox(width: 5),
                InkWell(
                  onTap: onMorePressed,
                  borderRadius: BorderRadius.circular(20),
                  child: const Padding(
                    padding: EdgeInsets.all(2),
                    child: Icon(
                      Icons.chevron_right,
                      size: 32,
                      color: Color(0xFF252B28),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),

        const SizedBox(height: 10),

        if (places.isEmpty)
          Container(
            height: 120,
            width: double.infinity,
            margin: const EdgeInsets.symmetric(horizontal: 20),
            decoration: BoxDecoration(
              color: const Color(0xFFF0F3F1),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(
                  Icons.location_off_outlined,
                  size: 32,
                  color: Color(0xFF9AA29E),
                ),
                const SizedBox(height: 8),
                Text(
                  emptyMessage,
                  style: const TextStyle(
                    fontFamily: 'Paperlogy',
                    fontSize: 14,
                    fontWeight: FontWeight.w300,
                    color: Color(0xFF7C8581),
                  ),
                ),
              ],
            ),
          )
        else
          SizedBox(
            height: 160,
            child: ListView.separated(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              scrollDirection: Axis.horizontal,
              physics: const BouncingScrollPhysics(),
              itemCount: visibleItemCount + (hasMore ? 1 : 0),
              separatorBuilder: (_, __) {
                return const SizedBox(width: 20);
              },
              itemBuilder: (context, index) {
                if (hasMore && index == visibleItemCount) {
                  return _MorePlaceCard(onTap: onMorePressed);
                }

                final place = places[index];

                return PlaceCard(place: place);
              },
            ),
          ),
      ],
    );
  }
}

// 장소 캐러셀 5개 이상 넘어가면 더보기 카드 하나 추가하는 위젯
class _MorePlaceCard extends StatelessWidget {
  const _MorePlaceCard({this.onTap});

  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 130,
      child: Material(
        color: const Color(0xFFF1F3F2),
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: const BoxDecoration(
                  color: Color(0xFF589C7E),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.add, color: Colors.white, size: 30),
              ),
              const SizedBox(height: 10),
              const Text(
                '더보기',
                style: TextStyle(
                  fontFamily: 'Paperlogy',
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: Color(0xFF252B28),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
