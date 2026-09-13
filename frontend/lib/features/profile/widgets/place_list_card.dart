import 'package:flutter/material.dart';

import '../models/place.dart';

class PlaceListCard extends StatelessWidget {
  const PlaceListCard({super.key, required this.place, this.onTap});

  final Place place;
  final VoidCallback? onTap;

  String? get formattedDate {
    final date = place.visitedAt;

    if (date == null) {
      return null;
    }

    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');

    return '${date.year}.$month.$day';
  }

  String get formattedRating {
    final rating = place.rating;

    if (rating == null) {
      return '';
    }

    return rating.toStringAsFixed(rating % 1 == 0 ? 0 : 1);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 160,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.1),
            blurRadius: 14,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          child: Row(
            children: [
              SizedBox(
                width: 150,
                height: double.infinity,
                child: Image.network(
                  place.imageUrl,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) {
                    return const ColoredBox(
                      color: Color(0xFFE9ECEF),
                      child: Center(
                        child: Icon(
                          Icons.image_not_supported_outlined,
                          size: 40,
                          color: Colors.grey,
                        ),
                      ),
                    );
                  },
                ),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(18, 20, 16, 18),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        place.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontFamily: 'Paperlogy',
                          fontSize: 16,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        place.address ?? '주소 정보 없음',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontFamily: 'Paperlogy',
                          fontSize: 14,
                          color: Color(0xFF6F7773),
                          fontWeight: FontWeight.w400,
                        ),
                      ),
                      const Spacer(),
                      Row(
                        children: [
                          if (place.rating != null) ...[
                            const Icon(
                              Icons.star,
                              size: 25,
                              color: Color(0xFF53A47B),
                            ),
                            const SizedBox(width: 3),
                            Text(
                              formattedRating,
                              style: const TextStyle(
                                fontFamily: 'Paperlogy',
                                fontSize: 15,
                              ),
                            ),
                          ],
                          const Spacer(),
                          if (formattedDate != null)
                            Text(
                              formattedDate!,
                              style: const TextStyle(
                                fontFamily: 'Paperlogy',
                                fontSize: 12,
                                color: Color(0xFF6F7773),
                              ),
                            ),
                        ],
                      ),
                    ],
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
