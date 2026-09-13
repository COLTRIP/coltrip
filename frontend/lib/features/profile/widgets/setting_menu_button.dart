import 'package:flutter/material.dart';

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
                      fontFamily: 'Paperlogy',
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
