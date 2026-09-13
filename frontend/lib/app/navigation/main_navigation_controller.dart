import 'package:get/get.dart';

class MainNavigationController extends GetxController {
  final RxInt currentIndex = 1.obs;

  void changeTab(int index) {
    currentIndex.value = index;
  }
}