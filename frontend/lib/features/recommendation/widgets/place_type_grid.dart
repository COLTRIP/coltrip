import 'package:flutter/material.dart';

import '../models/place_type_item.dart';
import 'place_type_card.dart';


class PlaceTypeGrid extends StatelessWidget {
  const PlaceTypeGrid({
    super.key,
    required this.items,
    required this.onSelected,
    this.selectedId,
  });

  final List<PlaceTypeItem> items;
  final String? selectedId;
  final ValueChanged<PlaceTypeItem> onSelected;

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      padding: const EdgeInsets.symmetric(
        horizontal: 20,
        vertical: 24,
      ),
      itemCount: items.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: 20,
        mainAxisSpacing: 20,
        childAspectRatio: 1.6,
      ),
      itemBuilder: (context, index) {
        final item = items[index];

        return PlaceTypeCard(
          item: item,
          isSelected: selectedId == item.id,
          onTap: () => onSelected(item),
        );
      },
    );
  }
}