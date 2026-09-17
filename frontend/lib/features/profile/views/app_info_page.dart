import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../../../shared/widgets/shared_app_bar.dart';

/// COLTRIP에서 사용하는 외부 데이터와 콘텐츠의 출처를 안내합니다.
class AppInfoPage extends StatelessWidget {
  const AppInfoPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      appBar: SharedAppBar(title: '정보', showBackButton: true),
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.fromLTRB(24, 28, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '데이터 출처',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF252B28),
                ),
              ),
              SizedBox(height: 18),
              _DataSourceRow(label: '사진 제공', source: '한국관광공사'),
              _DataSourceRow(label: '지도', source: 'NAVER'),
              _DataSourceRow(
                label: '혼잡도 데이터',
                source: 'SK텔레콤 지오비전 퍼즐',
              ),
              SizedBox(height: 28),
              Text(
                '앱 정보',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF252B28),
                ),
              ),
              SizedBox(height: 18),
              _AppVersionRow(),
            ],
          ),
        ),
      ),
    );
  }
}

class _AppVersionRow extends StatefulWidget {
  const _AppVersionRow();

  @override
  State<_AppVersionRow> createState() => _AppVersionRowState();
}

class _AppVersionRowState extends State<_AppVersionRow> {
  late final Future<PackageInfo> _packageInfo = PackageInfo.fromPlatform();

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<PackageInfo>(
      future: _packageInfo,
      builder: (context, snapshot) {
        final info = snapshot.data;
        final version = info == null
            ? '-'
            : '${info.version} (${info.buildNumber})';

        return _DataSourceRow(
          label: '버전',
          source: version,
          showDivider: false,
        );
      },
    );
  }
}

class _DataSourceRow extends StatelessWidget {
  const _DataSourceRow({
    required this.label,
    required this.source,
    this.showDivider = true,
  });

  final String label;
  final String source;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 18),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                width: 112,
                child: Text(
                  label,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: Color(0xFF59605D),
                  ),
                ),
              ),
              Expanded(
                child: Text(
                  source,
                  textAlign: TextAlign.right,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w400,
                    color: Color(0xFF252B28),
                  ),
                ),
              ),
            ],
          ),
        ),
        if (showDivider) const Divider(height: 1, color: Color(0x1F252B28)),
      ],
    );
  }
}
