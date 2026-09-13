import 'package:flutter/material.dart';

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
                      fontFamily: 'Paperlogy',
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
