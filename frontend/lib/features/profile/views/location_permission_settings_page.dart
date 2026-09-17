import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';

import '../../../shared/widgets/shared_app_bar.dart';

/// 현재 위치 권한 상태를 확인하고 시스템 설정에서 변경할 수 있는 화면입니다.
class LocationPermissionSettingsPage extends StatefulWidget {
  const LocationPermissionSettingsPage({super.key});

  @override
  State<LocationPermissionSettingsPage> createState() =>
      _LocationPermissionSettingsPageState();
}

class _LocationPermissionSettingsPageState
    extends State<LocationPermissionSettingsPage>
    with WidgetsBindingObserver {
  bool _isLoading = true;
  bool _isGranted = false;
  bool _isPermanentlyDenied = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refreshPermission();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _refreshPermission();
    }
  }

  Future<void> _refreshPermission() async {
    final permission = await Geolocator.checkPermission();
    if (!mounted) return;

    setState(() {
      _isGranted = permission == LocationPermission.whileInUse ||
          permission == LocationPermission.always;
      _isPermanentlyDenied = permission == LocationPermission.deniedForever;
      _isLoading = false;
    });
  }

  Future<void> _onPermissionChanged(bool enabled) async {
    if (!enabled || _isPermanentlyDenied) {
      await Geolocator.openAppSettings();
      return;
    }

    setState(() => _isLoading = true);
    await Geolocator.requestPermission();
    await _refreshPermission();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const SharedAppBar(
        title: '위치 권한 설정',
        showBackButton: true,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 28, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: const Color(0x1F252B28)),
                ),
                child: SwitchListTile(
                  value: _isGranted,
                  onChanged: _isLoading ? null : _onPermissionChanged,
                  activeThumbColor: const Color(0xFF589C7E),
                  title: const Text(
                    '현재 위치 접근',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: Color(0xFF252B28),
                    ),
                  ),
                  subtitle: Text(
                    _isLoading
                        ? '권한 상태를 확인하고 있어요.'
                        : _isGranted
                        ? '허용됨'
                        : '허용되지 않음',
                    style: const TextStyle(
                      fontSize: 13,
                      color: Color(0xFF747C78),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              const Text(
                '위치 권한은 장소 방문 여부를 확인할 때만 사용해요.\n'
                '권한을 끄려면 기기의 앱 설정 화면에서 위치 권한을 변경해주세요.',
                style: TextStyle(
                  fontSize: 14,
                  height: 1.6,
                  color: Color(0xFF747C78),
                ),
              ),
              const SizedBox(height: 16),
              TextButton.icon(
                onPressed: Geolocator.openAppSettings,
                icon: const Icon(Icons.settings_outlined),
                label: const Text('앱 설정에서 변경'),
                style: TextButton.styleFrom(
                  foregroundColor: const Color(0xFF589C7E),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
