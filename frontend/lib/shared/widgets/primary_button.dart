import 'package:flutter/material.dart';

class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.isOutlined = false,
    this.isLoading = false,
    this.buttonColor = const Color(0xFF589C7E),
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool isOutlined;
  final bool isLoading;
  final Color buttonColor;

  @override
  Widget build(BuildContext context) {
    final foregroundColor = isOutlined ? buttonColor : Colors.white;

    return SizedBox(
      width: double.infinity,
      height: 50,
      child: OutlinedButton(
        onPressed: isLoading ? null : onPressed,
        style: OutlinedButton.styleFrom(
          foregroundColor: foregroundColor,
          backgroundColor: isOutlined ? Colors.white : buttonColor,
          disabledForegroundColor: isOutlined
              ? buttonColor.withValues(alpha: 0.5)
              : Colors.white,
          disabledBackgroundColor: isOutlined
              ? Colors.white
              : buttonColor.withValues(alpha: 0.5),
          side: BorderSide(
            color: isLoading ? buttonColor.withValues(alpha: 0.5) : buttonColor,
            width: isOutlined ? 1.5 : 0,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
        child: isLoading
            ? CircularProgressIndicator(strokeWidth: 2, color: foregroundColor)
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
