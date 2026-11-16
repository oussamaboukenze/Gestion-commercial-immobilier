import 'package:flutter_test/flutter_test.dart';
import 'package:gecimmo_mobile/main.dart';

void main() {
  testWidgets("L'écran de connexion s'affiche", (tester) async {
    await tester.pumpWidget(const GecimmoApp());
    expect(find.text('GECIMMO'), findsOneWidget);
    expect(find.text('Se connecter'), findsOneWidget);
  });
}
