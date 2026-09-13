import '../features/profile/models/place_review.dart';

final dummyPlaceReviews = <int, PlaceReview>{
  1: PlaceReview(
    id: 101,
    rating: 4,
    content: '산책로가 조용하고 풍경이 예뻐서 좋았어요.',
    createdAt: DateTime(2026, 8, 12),
  ),
  2: PlaceReview(
    id: 102,
    rating: 4.5,
    content: '골목을 천천히 둘러보기 좋았어요.',
    createdAt: DateTime(2026, 8, 5),
  ),
  3: PlaceReview(
    id: 103,
    rating: 5,
    content: '바다를 보면서 쉬기 좋았고 야경도 예뻤어요.',
    createdAt: DateTime(2026, 7, 28),
  ),
  4: PlaceReview(
    id: 104,
    rating: 5,
    content: '',
    createdAt: DateTime(2026, 7, 28),
  ),
};
