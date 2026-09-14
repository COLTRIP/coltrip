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
