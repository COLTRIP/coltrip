import 'package:flutter/material.dart';
import 'package:flutter_naver_map/flutter_naver_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:get/get.dart';

import '../../../app/routes/app_routes.dart';
import '../services/map_spot_service.dart';
import '../widgets/map_search_bar.dart';

class MapPage extends StatefulWidget {
  const MapPage({super.key, this.isActive = true});

  final bool isActive;

  @override
  State<MapPage> createState() => _MapPageState();
}

class _MapPageState extends State<MapPage> {
  static const _mapSpotService = MapSpotService();

  bool _isLoadingSpots = false;
  final Map<String, NOverlayImage> _markerIconCache = {};

  Color _markerColor(String? quietLevel, int? quietScore) {
    var level = quietLevel;

    if (level == null && quietScore != null) {
      if (quietScore >= 71) {
        level = 'QUIET';
      } else if (quietScore >= 41) {
        level = 'NORMAL';
      } else {
        level = 'CROWDED';
      }
    }

    return switch (level) {
      'QUIET' => const Color(0xFF589C7E),
      'NORMAL' => const Color(0xFFE4A94B),
      'CROWDED' => const Color(0xFFC96363),
      _ => const Color(0xFF8C9691),
    };
  }

  Future<NOverlayImage> _markerIcon({
    required Color color,
    required int? quietScore,
  }) async {
    final label = quietScore?.clamp(0, 100).toString() ?? '?';
    final cacheKey = 'circle_${color.toARGB32()}_$label';
    final cachedIcon = _markerIconCache[cacheKey];

    if (cachedIcon != null) return cachedIcon;

    final icon = await NOverlayImage.fromWidget(
      context: context,
      size: const Size(44, 44),
      widget: Container(
        width: 44,
        height: 44,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: color,
          shape: BoxShape.circle,
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.22),
              blurRadius: 6,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Paperlogy',
            fontSize: label.length >= 3 ? 12 : 14,
            fontWeight: FontWeight.w700,
            color: Colors.white,
            height: 1,
          ),
        ),
      ),
    );

    _markerIconCache[cacheKey] = icon;
    return icon;
  }

  Future<void> _loadSpotsInCurrentBounds(NaverMapController controller) async {
    if (_isLoadingSpots) return;

    _isLoadingSpots = true;

    try {
      final bounds = await controller.getContentBounds();

      final spots = await _mapSpotService.findInBounds(
        swLat: bounds.southWest.latitude,
        swLng: bounds.southWest.longitude,
        neLat: bounds.northEast.latitude,
        neLng: bounds.northEast.longitude,
      );

      if (!mounted || !widget.isActive) return;

      final markers = <NAddableOverlay>{};

      for (final spot in spots) {
        final markerColor = _markerColor(spot.quietLevel, spot.quietScore);
        final markerIcon = await _markerIcon(
          color: markerColor,
          quietScore: spot.quietScore,
        );

        final marker = NMarker(
          id: 'spot_${spot.id}',
          position: NLatLng(spot.latitude, spot.longitude),
          icon: markerIcon,
          size: const Size(44, 44),
          anchor: const NPoint(0.5, 0.5),
          caption: NOverlayCaption(text: spot.name),
          captionOffset: 4,
        );

        marker.setOnTapListener((_) {
          debugPrint('선택한 장소: id=${spot.id}, name=${spot.name}');
          Get.toNamed(AppRoutes.recommendationDetail, arguments: spot.id);
        });

        markers.add(marker);
      }

      if (!mounted || !widget.isActive) return;

      await controller.clearOverlays(type: NOverlayType.marker);
      await controller.addOverlayAll(markers);

      debugPrint('현재 지도 범위 관광지 ${markers.length}개 표시 완료');
    } catch (error, stackTrace) {
      debugPrint('지도 관광지 조회 실패: $error\n$stackTrace');

      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('주변 관광지를 불러오지 못했어요.'),
          backgroundColor: Color(0xFF589C7E),
        ),
      );
    } finally {
      _isLoadingSpots = false;
    }
  }

  // 국민대학교 방문 로직 테스트용 초기 위치.
  // 부산 중심으로 되돌릴 때: NLatLng(35.1796, 129.0756)
  static const _initialCenter = NLatLng(37.6109, 126.9971);

  // 테스트 중에는 서울과 부산을 모두 이동할 수 있도록 전국 범위를 허용한다.
  static const _mapExtent = NLatLngBounds(
    southWest: NLatLng(33.0, 124.0),
    northEast: NLatLng(39.0, 132.0),
  );

  final _searchController = TextEditingController();
  final _searchFocusNode = FocusNode();

  NaverMapController? _mapController;
  bool _isSearching = false;
  bool _locationInitialized = false;

  @override
  void didUpdateWidget(covariant MapPage oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (!oldWidget.isActive && widget.isActive) {
      final controller = _mapController;

      if (controller != null) {
        _loadSpotsInCurrentBounds(controller);

        if (!_locationInitialized) {
          _initializeCurrentLocation(controller);
        }
      }
    }
  }

  Future<void> _initializeCurrentLocation(NaverMapController controller) async {
    final serviceEnabled = await Geolocator.isLocationServiceEnabled();

    if (!serviceEnabled) {
      _showLocationMessage('기기의 위치 서비스를 켜주세요.');
      return;
    }

    var permission = await Geolocator.checkPermission();

    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }

    if (permission == LocationPermission.deniedForever) {
      _showLocationMessage('설정에서 Coltrip의 위치 권한을 허용해주세요.');
      return;
    }

    if (permission == LocationPermission.denied) {
      _showLocationMessage('현위치를 사용하려면 위치 권한이 필요해요.');
      return;
    }

    controller.setLocationTrackingMode(NLocationTrackingMode.follow);
    _locationInitialized = true;
  }

  void _showLocationMessage(String message) {
    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: const Color(0xFF589C7E),
      ),
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    _searchFocusNode.dispose();
    super.dispose();
  }

  Future<void> _searchPlace(String keyword) async {
    final trimmedKeyword = keyword.trim();

    if (trimmedKeyword.isEmpty || _isSearching) return;

    _searchFocusNode.unfocus();

    setState(() {
      _isSearching = true;
    });

    try {
      // TODO: 백엔드 장소 검색 API 연결
      debugPrint('부산 장소 검색: $trimmedKeyword');
    } catch (error) {
      if (!mounted) return;

      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('장소를 검색하지 못했어요.')));
    } finally {
      if (mounted) {
        setState(() {
          _isSearching = false;
        });
      }
    }
  }

  void _clearSearch() {
    _searchController.clear();
    _searchFocusNode.requestFocus();
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        NaverMap(
          options: const NaverMapViewOptions(
            initialCameraPosition: NCameraPosition(
              target: _initialCenter,
              zoom: 15.5,
            ),
            extent: _mapExtent,
            minZoom: 6.5,
            maxZoom: 20,
            locationButtonEnable: true,
            compassEnable: false,
            scaleBarEnable: false,
          ),
          onMapReady: (controller) async {
            if (mounted) {
              setState(() {
                _mapController = controller;
              });
            }

            debugPrint('네이버 맵 로딩됨!');

            if (widget.isActive) {
              await _loadSpotsInCurrentBounds(controller);
              await _initializeCurrentLocation(controller);
            }
          },
          onCameraIdle: () {
            final controller = _mapController;

            if (widget.isActive && controller != null) {
              _loadSpotsInCurrentBounds(controller);
            }
          },
        ),

        // 검색창
        Positioned(
          top: MediaQuery.paddingOf(context).top + 12,
          left: 16,
          right: 16,
          child: MapSearchBar(
            controller: _searchController,
            focusNode: _searchFocusNode,
            isSearching: _isSearching,
            onSubmitted: _searchPlace,
            onChanged: (_) => setState(() {}),
            onClear: _clearSearch,
          ),
        ),

        Positioned(
          top: MediaQuery.paddingOf(context).top + 76,
          left: 16,
          child: const _QuietLevelLegend(),
        ),

        // 확대·축소 버튼
        Positioned(
          right: 16,
          bottom: 120,
          child: NaverMapZoomControlWidget(
            mapController: _mapController,
            size: 44,
            roundness: 8,
          ),
        ),
      ],
    );
  }
}

class _QuietLevelLegend extends StatelessWidget {
  const _QuietLevelLegend();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.92),
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.10),
            blurRadius: 8,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _LegendItem(color: Color(0xFF589C7E), label: '고요'),
          SizedBox(width: 12),
          _LegendItem(color: Color(0xFFE4A94B), label: '보통'),
          SizedBox(width: 12),
          _LegendItem(color: Color(0xFFC96363), label: '혼잡'),
        ],
      ),
    );
  }
}

class _LegendItem extends StatelessWidget {
  const _LegendItem({required this.color, required this.label});

  final Color color;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 9,
          height: 9,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 5),
        Text(
          label,
          style: const TextStyle(
            fontFamily: 'Paperlogy',
            fontSize: 12,
            color: Color(0xFF252B28),
          ),
        ),
      ],
    );
  }
}
