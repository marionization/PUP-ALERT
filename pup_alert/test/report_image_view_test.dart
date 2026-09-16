import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pup_alert/widgets/report_image_view.dart';

void main() {
  group('ReportImageView Tests', () {
    testWidgets('Displays fallback message when imageUrl is empty', (
      tester,
    ) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(body: ReportImageView(imageUrl: '')),
        ),
      );

      expect(find.text('No image available'), findsOneWidget);
      expect(find.byIcon(Icons.broken_image_outlined), findsOneWidget);
    });

    testWidgets('Renders base64 image data without throwing', (tester) async {
      // 1x1 transparent PNG base64
      const sampleBase64 =
          'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(body: ReportImageView(imageUrl: sampleBase64)),
        ),
      );

      expect(find.byType(Image), findsOneWidget);
    });

    testWidgets(
      'Tap to enlarge opens interactive dialog when enableZoom is true',
      (tester) async {
        const sampleBase64 =
            'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';

        await tester.pumpWidget(
          const MaterialApp(
            home: Scaffold(
              body: ReportImageView(
                imageUrl: sampleBase64,
                width: 200,
                height: 200,
                enableZoom: true,
              ),
            ),
          ),
        );

        // Tap to enlarge
        await tester.tap(find.byType(ReportImageView));
        await tester.pumpAndSettle();

        // Verify zoom dialog opened with close icon and InteractiveViewer
        expect(find.byType(InteractiveViewer), findsOneWidget);
        expect(find.byIcon(Icons.close), findsOneWidget);

        // Close dialog
        await tester.tap(find.byIcon(Icons.close));
        await tester.pumpAndSettle();

        expect(find.byType(InteractiveViewer), findsNothing);
      },
    );
  });
}
