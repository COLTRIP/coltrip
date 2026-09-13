import 'package:flutter/material.dart';
import 'package:flutter_naver_map/flutter_naver_map.dart';

import '../widgets/map_search_bar.dart';


class MapPage extends StatefulWidget {
  const MapPage({super.key});

  @override
  State<MapPage> createState() => _MapPageState();
}

class _MapPageState extends State<MapPage> {
  static const _busanCenter = NLatLng(35.1796, 129.0756);

  static const _busanExtent = NLatLngBounds(
    southWest: NLatLng(34.85, 128.70),
    northEast: NLatLng(35.45, 129.45),
  );

  final _searchController = TextEditingController();
  final _searchFocusNode = FocusNode();

  NaverMapController? _mapController;
  bool _isSearching = false;

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

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('장소를 검색하지 못했어요.'),
        ),
      );
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
              target: _busanCenter,
              zoom: 10.5,
            ),
            extent: _busanExtent,
            minZoom: 9.5,
            maxZoom: 20,
            locationButtonEnable: true,
            compassEnable: false,
            scaleBarEnable: false,
          ),
          onMapReady: (controller) {
            setState(() {
              _mapController = controller;
            });

            debugPrint('네이버 맵 로딩됨!');
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