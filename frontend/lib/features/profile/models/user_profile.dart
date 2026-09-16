/// 사용자 프로필 정보를 나타내는 모델입니다.
///
/// 사용자의 닉네임과 방문한 장소 및 좋아요한 장소의 개수를 관리합니다.
class UserProfile {
  const UserProfile({
    required this.nickname,
    required this.visitCount,
    required this.likeCount,
  });

  final String nickname;
  final int visitCount;
  final int likeCount;

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      nickname: json['nickname'] as String? ?? '',
      visitCount: (json['visitCount'] as num?)?.toInt() ?? 0,
      likeCount: (json['likeCount'] as num?)?.toInt() ?? 0,
    );
  }
}
