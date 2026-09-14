import 'package:flutter/material.dart';

import '../models/place_type_item.dart';

class PlaceTypeCard extends StatelessWidget {
  const PlaceTypeCard({
    super.key,
    required this.item,
    required this.onTap,
    this.isSelected = false,
  });

  final PlaceTypeItem item;
  final VoidCallback onTap;
  final bool isSelected;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: item.color,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: isSelected
                  ? const Color(0xFF589C7E)
                  : const Color(0xFF589C7E).withValues(alpha: 0.25),
              width: isSelected ? 2.5 : 1.2,
            ),
          ),
          child: Center(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  item.label,
                  style: const TextStyle(
                    fontFamily: 'Paperlogy',
                    color: Color(0xFF252B28),
                    fontSize: 15,
                    fontWeight: FontWeight.w400,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}