import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';

import '../../../../shared/widgets/app_button.dart';
import '../../view_models/location_permission_view_model.dart';

// 방문 시작 전 위치 권한을 요청하는 화면.
// pop(true) = 허용됨, pop(false)/pop(null) = 거부 또는 그냥 닫음
class LocationPermissionPage extends StatefulWidget {
  const LocationPermissionPage({super.key});

  @override
  State<LocationPermissionPage> createState() => _LocationPermissionPageState();
}

class _LocationPermissionPageState extends State<LocationPermissionPage> with WidgetsBindingObserver {
  final _viewModel = LocationPermissionViewModel();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _viewModel.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _checkPermissionOnResume();
    }
  }

  Future<void> _checkPermissionOnResume() async {
    await _viewModel.checkPermissionOnResume();
    if (_viewModel.granted && mounted) {
      Get.back(result: true);
    }
  }

  Future<void> _requestPermission() async {
    await _viewModel.requestPermission();
    if (_viewModel.granted && mounted) {
      Get.back(result: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            children: [
              Align(
                alignment: Alignment.topRight,
                child: IconButton(
                  onPressed: () => Get.back(result: false),
                  icon: const Icon(Icons.close, color: Colors.black),
                ),
              ),
              const Spacer(flex: 3),
              SvgPicture.asset('assets/icons/location_pin.svg', width: 64, height: 80),
              const SizedBox(height: 30),
              const Text(
                '방문 확인을 위해 위치가 필요해요',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'Paperlogy',
                  fontSize: 22,
                  fontWeight: FontWeight.w600,
                  color: Colors.black,
                ),
              ),
              const SizedBox(height: 24),
              const Text(
                '장소 방문을 시작하면 현재 위치를 확인해\n실제로 장소에 방문했는지 확인해요.\n\n'
                '위치 권한은 처음 한 번만 요청하며,\n방문 중 필요한 순간에만 위치를 확인해요.\n\n'
                '방문이 완료되거나 취소되면\n더 이상 위치를 확인하지 않아요.',
                textAlign: TextAlign.center,
                style: TextStyle(fontFamily: 'Paperlogy', fontSize: 15, color: Colors.black),
              ),
              const Spacer(flex: 4),
              ListenableBuilder(
                listenable: _viewModel,
                builder: (context, _) {
                  return Column(
                    children: [
                      if (_viewModel.errorMessage != null) ...[
                        Text(
                          _viewModel.errorMessage!,
                          textAlign: TextAlign.center,
                          style: const TextStyle(fontFamily: 'Paperlogy', fontSize: 12, color: Colors.red),
                        ),
                        const SizedBox(height: 8),
                      ],
                      PrimaryButton(
                        label: _viewModel.isPermanentlyDenied ? '설정으로 이동' : '위치 엑세스 허용',
                        onPressed: _viewModel.isPermanentlyDenied ? _viewModel.openAppSettings : _requestPermission,
                      ),
                    ],
                  );
                },
              ),
              const SizedBox(height: 12),
            ],
          ),
        ),
      ),
    );
  }
}
