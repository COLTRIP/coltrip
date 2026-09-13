// 방문 중 화면(VisitingSpotPage)의 진행 상태. 서버 모델이 아니라 프론트 뷰 상태.
enum VisitStatus {
  visiting, // 방문 중 (평상시)
  crowdingDetected, // 고요지수 하락 감지
  notAtSpot, // 방문 완료 시도했으나 목적지 반경 밖 (잠깐 떴다가 visiting으로 복귀)
  completed, // 방문 완료
}
