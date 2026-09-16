import 'package:flutter/material.dart';

/// 계정 관리 화면에서 사용하는 메뉴 버튼입니다.
///
/// 메뉴 항목을 표시하고, 위험 작업 여부에 따라 텍스트 스타일을 구분하여 제공합니다.
class AccountMenuButton extends StatelessWidget {
  const AccountMenuButton({
    super.key,
    required this.title,
    required this.onTap,
    this.showDivider = true,
    this.isDanger = false,
  });

  final String title;
  final VoidCallback onTap;
  final bool showDivider;
  final bool isDanger;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: Column(
          children: [
            SizedBox(
              height: 84,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 28),
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    title,
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w500,
                      color: isDanger
                          ? const Color(0xFFC96363)
                          : const Color(0xFF252B28),
                    ),
                  ),
                ),
              ),
            ),
            if (showDivider)
              const Divider(
                height: 1,
                thickness: 0.5,
                indent: 28,
                endIndent: 28,
                color: Color(0x33252B28),
              ),
          ],
        ),
      ),
    );
  }
}
