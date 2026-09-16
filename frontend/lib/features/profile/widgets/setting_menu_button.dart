import 'package:flutter/material.dart';

/// 프로필 화면에서 사용하는 설정 메뉴 버튼입니다.
///
/// 메뉴 이름과 이동 아이콘을 표시하고, 선택 시 지정된 동작을 실행합니다.
class SettingMenuButton extends StatelessWidget {
  const SettingMenuButton({
    super.key,
    required this.title,
    required this.onTap,
  });

  final String title;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: SizedBox(
          height: 60,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 30),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      color: Color(0xFF252B28),
                    ),
                  ),
                ),
                const Icon(
                  Icons.chevron_right,
                  size: 32,
                  color: Color(0xFF252B28),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
