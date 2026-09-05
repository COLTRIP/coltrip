import 'package:flutter/material.dart';

import '../../../view_models/recommendation_filter_view_model.dart';

class RecommendationFilterSheet extends StatelessWidget {
  final RecommendationFilterViewModel viewModel;

  const RecommendationFilterSheet({super.key, required this.viewModel});

  Future<void> _changeDate(BuildContext context) async {
    final current = viewModel.filter.dateTime;
    final date = await showDatePicker(
      context: context,
      initialDate: current,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (date == null || !context.mounted) return;

    viewModel.updateDateTime(
      DateTime(date.year, date.month, date.day, current.hour, current.minute),
    );
  }

  Future<void> _changeTime(BuildContext context) async {
    final current = viewModel.filter.dateTime;
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(current),
    );
    if (time == null || !context.mounted) return;

    viewModel.updateDateTime(
      DateTime(
        current.year,
        current.month,
        current.day,
        time.hour,
        time.minute,
      ),
    );
  }

  Future<void> _changeCategory(BuildContext context) async {
    // TODO: 실제 카테고리 선택 화면 만들면 Get.to로 이동해서 결과값 받기
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFAFAFA),
        border: Border.all(color: const Color(0xFFE5E5E5)),
        borderRadius: BorderRadius.circular(10),
      ),
      padding: const EdgeInsets.all(15),

      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [

          const Text(
            '장소 유형',
            style: TextStyle(
              fontFamily: 'Paperlogy',
              fontSize: 10,
              fontWeight: FontWeight.w500,
              color: Color(0xFF6F7773),

            ),
          ), const SizedBox(height: 4,),
          GestureDetector(
            onTap: () => _changeCategory(context),
            child: Row(
              children: [
                Text(
                  viewModel.filter.category,
                  style: const TextStyle(
                    fontFamily: 'Paperlogy',
                    fontSize: 24,
                    fontWeight: FontWeight.w500,
                    color: Colors.black,
                  ),
                ),
                const Icon(Icons.chevron_right, color: Color(0xFF474444)),
              ],
            ),
          ),
          const SizedBox(height: 7),
          const Divider(thickness: 0.5, color: Color(0x33252B28)),
          const SizedBox(height: 7),
          Row(
            children: [
              Expanded(
                child: _FilterBox(
                  // 날짜
                  value: viewModel.formattedDate,
                  icon: Icons.calendar_month,
                  onTap: () => _changeDate(context),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _FilterBox(
                  // 시간
                  value: viewModel.formattedTime,
                  icon: Icons.access_time,
                  onTap: () => _changeTime(context),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _FilterBox extends StatelessWidget {
  final String value;
  final IconData icon;
  final VoidCallback onTap;

  const _FilterBox({required this.value, required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(10),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
        decoration: BoxDecoration(
          color: const Color(0xFFFAFAFA),
          border: Border.all(color: const Color(0xFFE5E5E5)),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              value,
              style: const TextStyle(
                fontFamily: 'Paperlogy',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: Colors.black,
              ),
            ),
            Icon(icon, size: 20, color: const Color(0xFF474444)),
          ],
        ),
      ),
    );
  }
}
