import 'package:flutter/material.dart';

import '../models/place_mood_item.dart';

class PlaceMoodSelector extends StatelessWidget {
  const PlaceMoodSelector({
    super.key,
    required this.items,
    required this.selectedMoodIds,
    required this.onChanged,
  });

  final List<PlaceMoodItem> items;
  final Set<String> selectedMoodIds;
  final ValueChanged<PlaceMoodItem> onChanged;

  @override
  Widget build(BuildContext context) {
    if (items.length < 8) {
      return const SizedBox.shrink();
    }

    // 디자인에 맞게 3 / 2 / 3 배치
    final moodRows = [
      items.sublist(0, 3),
      items.sublist(3, 5),
      items.sublist(5, 8),
    ];

    return LayoutBuilder(
      builder: (context, constraints) {
        const horizontalSpacing = 16.0;
        const verticalSpacing = 24.0;

        final buttonWidth = (constraints.maxWidth - horizontalSpacing * 2) / 3;

        return Column(
          children: [
            for (var rowIndex = 0; rowIndex < moodRows.length; rowIndex++) ...[
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  for (
                    var index = 0;
                    index < moodRows[rowIndex].length;
                    index++
                  ) ...[
                    if (index > 0) const SizedBox(width: horizontalSpacing),

                    SizedBox(
                      width: buttonWidth,
                      child: _MoodButton(
                        label: moodRows[rowIndex][index].label,
                        isSelected: selectedMoodIds.contains(
                          moodRows[rowIndex][index].id,
                        ),
                        onTap: () {
                          onChanged(moodRows[rowIndex][index]);
                        },
                      ),
                    ),
                  ],
                ],
              ),

              if (rowIndex < moodRows.length - 1)
                const SizedBox(height: verticalSpacing),
            ],
          ],
        );
      },
    );
  }
}

class _MoodButton extends StatelessWidget {
  const _MoodButton({
    required this.label,
    required this.isSelected,
    required this.onTap,
  });

  final String label;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 180),
      height: 50,
      decoration: BoxDecoration(
        color: isSelected ? const Color(0xFF5CA887) : const Color(0xFFE5E5E5),
        borderRadius: BorderRadius.circular(28),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.14),
            blurRadius: 7,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(28),
          child: Center(
            child: Text(
              label,
              style: const TextStyle(
                fontFamily: 'Paperlogy',
                fontSize: 17,
                fontWeight: FontWeight.w400,
                color: Color(0xFF252B28),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
