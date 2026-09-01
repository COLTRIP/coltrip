import 'package:flutter/material.dart';


class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.isOutlined = false,
    this.isLoading = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool isOutlined;
  final bool isLoading;

  static const Color _primaryColor = Color(0xFF589C7E);

  @override
  Widget build(BuildContext context) {
    final foregroundColor =
    isOutlined ? _primaryColor : Colors.white;

    return SizedBox(
      width: double.infinity,
      height: 50,
      child: OutlinedButton(
        onPressed: isLoading ? null : onPressed,
        style: OutlinedButton.styleFrom(
          foregroundColor: foregroundColor,
          backgroundColor:
          isOutlined ? Colors.white : _primaryColor,
          disabledBackgroundColor:
          isOutlined ? Colors.white : _primaryColor.withOpacity(0.5),
          side: BorderSide(
            color: _primaryColor,
            width: isOutlined ? 1.5 : 0,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
        child: isLoading
            ? CircularProgressIndicator(
          strokeWidth: 2,
          color: foregroundColor,
        )
            : Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 28),
              const SizedBox(width: 12),
            ],
            Text(
              label,
              style: const TextStyle(
                fontFamily: 'Paperlogy',
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}