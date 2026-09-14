import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../models/place.dart';
import '../models/place_review.dart';

void showPlaceDetailBottomSheet({
  required Place place,
  PlaceReview? review,
  VoidCallback? onEditReview,
  VoidCallback? onDeleteReview,
}) {
  Get.bottomSheet(
    PlaceDetailBottomSheet(
      place: place,
      review: review,
      onEditReview: onEditReview,
      onDeleteReview: onDeleteReview,
    ),
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    barrierColor: Colors.black.withValues(alpha: 0.45),
  );
}

class PlaceDetailBottomSheet extends StatelessWidget {
  const PlaceDetailBottomSheet({
    super.key,
    required this.place,
    this.review,
    this.onEditReview,
    this.onDeleteReview,
  });

  final Place place;
  final PlaceReview? review;
  final VoidCallback? onEditReview;
  final VoidCallback? onDeleteReview;

  String _formatDate(DateTime date) {
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');

    return '${date.year}.$month.$day';
  }

  String _formatRating(double rating) {
    return rating.toStringAsFixed(rating % 1 == 0 ? 0 : 1);
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: FractionallySizedBox(
        heightFactor: 0.8,
        child: Container(
          decoration: const BoxDecoration(
            color: Color(0xFFF7F9F8),
            borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
          ),
          child: Column(
            children: [
              const SizedBox(height: 10),

              // 바텀시트 드래그 핸들
              Container(
                width: 42,
                height: 4,
                decoration: BoxDecoration(
                  color: const Color(0xFFD6DAD8),
                  borderRadius: BorderRadius.circular(10),
                ),
              ),

              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(24, 14, 24, 30),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 20),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(20),
                        child: AspectRatio(
                          aspectRatio: 16 / 10,
                          child: Image.network(
                            place.imageUrl,
                            width: double.infinity,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) {
                              return const ColoredBox(
                                color: Color(0xFFE9ECEF),
                                child: Center(
                                  child: Icon(
                                    Icons.image_not_supported_outlined,
                                    size: 48,
                                    color: Color(0xFF8A918E),
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                      ),

                      const SizedBox(height: 24),

                      Text(
                        place.name,
                        style: const TextStyle(
                          fontFamily: 'Paperlogy',
                          fontSize: 24,
                          fontWeight: FontWeight.w600,
                          color: Color(0xFF252B28),
                        ),
                      ),

                      const SizedBox(height: 6),

                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Icon(
                            Icons.location_on_outlined,
                            size: 19,
                            color: Color(0xFF737B77),
                          ),
                          const SizedBox(width: 4),
                          Expanded(
                            child: Text(
                              place.address ?? '주소 정보 없음',
                              style: const TextStyle(
                                fontFamily: 'Paperlogy',
                                fontSize: 14,
                                fontWeight: FontWeight.w300,
                                color: Color(0xFF737B77),
                              ),
                            ),
                          ),
                        ],
                      ),

                      const SizedBox(height: 20),
                      const Divider(color: Color(0x33252B28)),
                      const SizedBox(height: 20),

                      if (review != null) ...[
                        const SizedBox(height: 20),

                        const Text(
                          '내 리뷰',
                          style: TextStyle(
                            fontFamily: 'Paperlogy',
                            fontSize: 18,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF252B28),
                          ),
                        ),

                        const SizedBox(height: 14),

                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(18),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(18),
                            border: Border.all(color: const Color(0xFFE4E8E6)),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  const Icon(
                                    Icons.star_rounded,
                                    size: 28,
                                    color: Color(0xFF589C7E),
                                  ),
                                  const SizedBox(width: 4),
                                  Text(
                                    _formatRating(review!.rating),
                                    style: const TextStyle(
                                      fontFamily: 'Paperlogy',
                                      fontSize: 17,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                  const Spacer(),
                                  Text(
                                    _formatDate(review!.createdAt),
                                    style: const TextStyle(
                                      fontFamily: 'Paperlogy',
                                      fontSize: 13,
                                      color: Color(0xFF858C89),
                                    ),
                                  ),
                                ],
                              ),

                              const SizedBox(height: 14),

                              if (review!.content.trim().isNotEmpty)
                                Text(
                                  review!.content,
                                  style: const TextStyle(
                                    fontFamily: 'Paperlogy',
                                    fontSize: 15,
                                    fontWeight: FontWeight.w300,
                                    height: 1.55,
                                    color: Color(0xFF252B28),
                                  ),
                                )
                              else
                                const Text(
                                  '한줄평 없이 별점만 남겼어요.',
                                  style: TextStyle(
                                    fontFamily: 'Paperlogy',
                                    fontSize: 14,
                                    fontWeight: FontWeight.w300,
                                    color: Color(0xFF929996),
                                  ),
                                ),

                              if (onEditReview != null ||
                                  onDeleteReview != null) ...[
                                const SizedBox(height: 10),

                                Align(
                                  alignment: Alignment.bottomRight,
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      if (onEditReview != null)
                                        TextButton(
                                          onPressed: onEditReview,
                                          style: TextButton.styleFrom(
                                            minimumSize: Size.zero,
                                            padding: const EdgeInsets.symmetric(
                                              horizontal: 8,
                                              vertical: 4,
                                            ),
                                            tapTargetSize: MaterialTapTargetSize
                                                .shrinkWrap,
                                          ),
                                          child: const Text(
                                            '수정',
                                            style: TextStyle(
                                              fontFamily: 'Paperlogy',
                                              fontSize: 14,
                                              fontWeight: FontWeight.w500,
                                              color: Color(0xFF589C7E),
                                            ),
                                          ),
                                        ),

                                      const SizedBox(width: 8),

                                      if (onDeleteReview != null)
                                        TextButton(
                                          onPressed: onDeleteReview,
                                          style: TextButton.styleFrom(
                                            minimumSize: Size.zero,
                                            padding: const EdgeInsets.symmetric(
                                              horizontal: 8,
                                              vertical: 4,
                                            ),
                                            tapTargetSize: MaterialTapTargetSize
                                                .shrinkWrap,
                                          ),
                                          child: const Text(
                                            '삭제',
                                            style: TextStyle(
                                              fontFamily: 'Paperlogy',
                                              fontSize: 14,
                                              fontWeight: FontWeight.w500,
                                              color: Color(0xFFE15D5D),
                                            ),
                                          ),
                                        ),
                                    ],
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                      ],
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
