import 'package:flutter/foundation.dart';

import '../exceptions/recommendation_exception.dart';
import '../models/quiet_score_point.dart';
import '../models/recommendation.dart';
import '../models/review.dart';

class RecommendationDetailViewModel extends ChangeNotifier {
  final int spotId;

  SpotDetail? spot;
  bool isLoading = false;
  String? errorMessage;

  RecommendationDetailViewModel({required this.spotId}) {
    loadDetail();
  }

  Future<void> loadDetail() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();

    try {
      spot = await _fetchDummyDetail(spotId); // TODO: 실제 API 연결되면 repository 호출로 교체
    } on RecommendationLoadException catch (e) {
      errorMessage = e.message;
    } catch (e) {
      errorMessage = const RecommendationLoadException('상세 정보를 불러오지 못했어요. 다시 시도해주세요.').message;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<SpotDetail> _fetchDummyDetail(int spotId) async {
    await Future.delayed(const Duration(milliseconds: 300));
    return SpotDetail(
      id: spotId,
      name: '을숙도 생태공원',
      address: '부산광역시 사하구 하단동 1207',
      category: 'PARK',
      modes: const ['CONTEMPLATION', 'SCENERY'],
      description: '낙동강 하구에 위치한 생태공원으로, 철새 도래지로 유명해 조용히 산책하기 좋은 곳입니다.',
      imageUrl: 'https://placehold.co/400x300/png?text=Spot+$spotId',
      recommendReason: '조용한 골목 안쪽에 위치해 방문객이 적고, 사유하기 좋은 공간입니다',
      latitude: 35.1039,
      longitude: 128.9553,
      quietScore: 98,
      quietScoreUpdatedAt: DateTime.now(),
      quietScoreTimeline: const [
        QuietScorePoint(hour: 0, score: 55),
        QuietScorePoint(hour: 1, score: 40),
        QuietScorePoint(hour: 2, score: 65),
        QuietScorePoint(hour: 3, score: 45),
        QuietScorePoint(hour: 4, score: 75),
        QuietScorePoint(hour: 5, score: 90),
        QuietScorePoint(hour: 6, score: 98),
        QuietScorePoint(hour: 7, score: 95),
        QuietScorePoint(hour: 8, score: 72),
        QuietScorePoint(hour: 9, score: 30),
        QuietScorePoint(hour: 10, score: 42),
        QuietScorePoint(hour: 11, score: 60),
        QuietScorePoint(hour: 12, score: 55),
        QuietScorePoint(hour: 13, score: 48),
        QuietScorePoint(hour: 14, score: 62),
        QuietScorePoint(hour: 15, score: 70),
        QuietScorePoint(hour: 16, score: 58),
        QuietScorePoint(hour: 17, score: 35),
        QuietScorePoint(hour: 18, score: 28),
        QuietScorePoint(hour: 19, score: 50),
        QuietScorePoint(hour: 20, score: 68),
        QuietScorePoint(hour: 21, score: 80),
        QuietScorePoint(hour: 22, score: 88),
        QuietScorePoint(hour: 23, score: 72),
      ],
      reviews: [
        SpotReview(
          userName: '사용자님',
          rating: 4,
          date: DateTime(2026, 8, 12),
          content: '리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰리뷰',
        ),
        SpotReview(
          userName: '조용한여행러',
          rating: 5,
          date: DateTime(2026, 8, 10),
          content: '사람이 거의 없어서 진짜 조용했어요. 산책하기 딱 좋았습니다.',
        ),
        SpotReview(
          userName: '고요한하루',
          rating: 3,
          date: DateTime(2026, 8, 5),
          content: '주차하기가 조금 불편했지만 그 외엔 만족스러웠어요.',
        ),
      ],
    );
  }
}
