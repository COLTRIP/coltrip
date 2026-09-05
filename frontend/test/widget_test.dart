import 'package:flutter_test/flutter_test.dart';

import 'package:coltrip/main.dart';

void main() {
  testWidgets('App renders home page', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('Coltrip'), findsWidgets);
  });
}
