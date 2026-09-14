import 'package:flutter/material.dart';

import '../models/place_type_item.dart';

class PlaceTypeCard extends StatelessWidget {
  const PlaceTypeCard({
    super.key,
    required this.item,
    required this.onTap,
    this.isSelected = false,
    this.hasSelection = false,
  });

  final PlaceTypeItem item;
  final VoidCallback onTap;
  final bool isSelected;
  final bool hasSelection;

  static const primaryColor = Color(0xFF589C7E);

  @override
  Widget build(BuildContext context) {
    final isDimmed = hasSelection && !isSelected;

    return AnimatedOpacity(
      duration: const Duration(milliseconds: 180),
      opacity: isDimmed ? 0.4 : 1,
      child: AnimatedScale(
        duration: const Duration(milliseconds: 180),
        scale: isSelected ? 1.04 : 1,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          decoration: BoxDecoration(
            color: item.color,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: isSelected ? primaryColor : Colors.transparent,
              width: 3,
            ),
            boxShadow: [
              BoxShadow(
                color: isSelected
                    ? primaryColor.withValues(alpha: 0.35)
                    : Colors.black.withValues(alpha: 0.12),
                blurRadius: isSelected ? 12 : 6,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Material(
            color: Colors.transparent,
            borderRadius: BorderRadius.circular(16),
            child: InkWell(
              onTap: onTap,
              borderRadius: BorderRadius.circular(16),
              child: Stack(
                children: [
                  Center(
                    child: Text(
                      item.label,
                      style: TextStyle(
                        fontFamily: 'Paperlogy',
                        color: const Color(0xFF252B28),
                        fontSize: 15,
                        fontWeight: isSelected
                            ? FontWeight.w600
                            : FontWeight.w400,
                      ),
                    ),
                  ),

                  if (isSelected)
                    const Positioned(
                      top: 8,
                      right: 8,
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          color: primaryColor,
                          shape: BoxShape.circle,
                        ),
                        child: Padding(
                          padding: EdgeInsets.all(3),
                          child: Icon(
                            Icons.check,
                            size: 15,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
