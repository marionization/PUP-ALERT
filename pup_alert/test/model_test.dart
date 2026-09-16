import 'package:flutter_test/flutter_test.dart';
import 'package:pup_alert/models/report_model.dart';
import 'package:pup_alert/models/review_model.dart';
import 'package:pup_alert/models/survey_model.dart';
import 'package:pup_alert/services/ai_service.dart';

void main() {
  group('Report Model Tests', () {
    test('Report serializes to and from Firestore map properly', () {
      final report = Report(
        id: 'test_id_123',
        title: 'Broken Light in Lab',
        category: 'Electrical Hazard',
        location: 'Main Bldg Room 301',
        description: 'Fluorescent lamp is flickering and sparking',
        mediaUrl: 'https://example.com/photo.jpg',
        mediaType: 'image',
        status: 'In Review',
        dateSubmitted: '2026-09-16 14:00',
        reporter: 'Juan Dela Cruz',
        timestamp: 1726466400000,
        averageRating: 4.5,
        ratingCount: 2,
        isAnonymous: false,
        reporterUid: 'user_123',
        reporterStudentId: '2021-00001-MN-0',
      );

      final map = report.toFirestore();
      expect(map['title'], 'Broken Light in Lab');
      expect(map['category'], 'Electrical Hazard');
      expect(map['status'], 'In Review');

      final fromMap = Report.fromFirestore(map, 'test_id_123');
      expect(fromMap.id, 'test_id_123');
      expect(fromMap.title, report.title);
      expect(fromMap.location, report.location);
      expect(fromMap.averageRating, 4.5);
    });

    test('Anonymous report retains reporterUid but flags anonymity', () {
      final report = Report(
        id: 'anon_1',
        title: 'Safety issue',
        isAnonymous: true,
        reporter: 'Anonymous Student',
        reporterUid: 'secret_uid',
      );

      expect(report.isAnonymous, isTrue);
      expect(report.reporter, 'Anonymous Student');
    });

    test('Report handles mediaUrl, imageUrl, hasImage, and displayImageUrl consistently', () {
      final reportWithMedia = Report.fromFirestore({
        'title': 'Cracked Wall',
        'mediaUrl': 'https://firebasestorage.googleapis.com/v0/b/img.jpg',
      }, 'rep_1');
      expect(reportWithMedia.hasImage, isTrue);
      expect(
        reportWithMedia.displayImageUrl,
        'https://firebasestorage.googleapis.com/v0/b/img.jpg',
      );

      final reportWithImageUrlOnly = Report.fromFirestore({
        'title': 'Broken Window',
        'imageUrl': 'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD',
        'mediaUrl': '',
      }, 'rep_2');
      expect(reportWithImageUrlOnly.hasImage, isTrue);
      expect(
        reportWithImageUrlOnly.displayImageUrl,
        'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD',
      );

      final reportWithoutImage = Report.fromFirestore({
        'title': 'Noise complaint',
      }, 'rep_3');
      expect(reportWithoutImage.hasImage, isFalse);
      expect(reportWithoutImage.displayImageUrl, '');

      final reportWithBoth = Report.fromFirestore({
        'title': 'Flickering Bulb',
        'mediaUrl': 'https://firebasestorage.googleapis.com/v0/b/bulb.jpg',
        'imageUrl': 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
      }, 'rep_4');
      expect(reportWithBoth.hasImage, isTrue);
      expect(
        reportWithBoth.displayImageUrl,
        'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
      );
    });
  });

  group('Review Model Tests', () {
    test('Review serialization test', () {
      final review = Review(
        id: 'rev_1',
        rating: 5,
        text: 'Issue was resolved very quickly!',
        reviewerName: 'Maria Santos',
        reviewerStudentId: '2021-00002-MN-0',
        likes: 3,
        dislikes: 0,
      );

      final map = review.toFirestore();
      expect(map['rating'], 5);
      expect(map['text'], 'Issue was resolved very quickly!');

      final fromMap = Review.fromFirestore(map, 'rev_1');
      expect(fromMap.rating, 5);
      expect(fromMap.likes, 3);
    });

    test('Review with admin reply serialization and deserialization', () {
      final review = Review(
        id: 'rev_admin_1',
        rating: 4,
        text: 'Water leakage in corridor was fixed promptly.',
        reviewerName: 'John Student',
        reviewerStudentId: '2022-00123-MN-0',
        likes: 5,
        dislikes: 0,
        timestamp: 1726466400000,
        adminReply: 'Thank you John! Facilities team replaced the joint pipe.',
        adminRepliedAt: 1726470000000,
        adminReplierName: 'Admin Safety Dept',
      );

      final map = review.toFirestore();
      expect(
        map['adminReply'],
        'Thank you John! Facilities team replaced the joint pipe.',
      );
      expect(map['adminRepliedAt'], 1726470000000);
      expect(map['adminReplierName'], 'Admin Safety Dept');

      final deserialized = Review.fromFirestore(map, 'rev_admin_1');
      expect(deserialized.id, 'rev_admin_1');
      expect(deserialized.adminReply, review.adminReply);
      expect(deserialized.adminRepliedAt, 1726470000000);
      expect(deserialized.adminReplierName, 'Admin Safety Dept');
    });
  });

  group('PerformanceSurvey Model Tests', () {
    test('PerformanceSurvey serializes to and from Firestore map properly', () {
      final survey = PerformanceSurvey(
        id: 'survey_123',
        reportId: 'rep_456',
        reportTitle: 'Broken Ceiling Fan in Room 402',
        studentUid: 'uid_student_01',
        studentName: 'Ana Gomez',
        studentId: '2023-00999-MN-0',
        overallRating: 5,
        resolutionSpeed: 'Fast (Within 24 Hours)',
        staffProfessionalism: 'Excellent',
        safetyImprovement: 'Significantly Safer',
        comments: 'Technicians were polite and tested the fan before leaving.',
        timestamp: 1726468000000,
      );

      final map = survey.toFirestore();
      expect(map['reportId'], 'rep_456');
      expect(map['reportTitle'], 'Broken Ceiling Fan in Room 402');
      expect(map['overallRating'], 5);
      expect(map['resolutionSpeed'], 'Fast (Within 24 Hours)');
      expect(map['staffProfessionalism'], 'Excellent');
      expect(map['safetyImprovement'], 'Significantly Safer');
      expect(
        map['comments'],
        'Technicians were polite and tested the fan before leaving.',
      );

      final fromMap = PerformanceSurvey.fromFirestore(map, 'survey_123');
      expect(fromMap.id, 'survey_123');
      expect(fromMap.reportId, 'rep_456');
      expect(fromMap.studentName, 'Ana Gomez');
      expect(fromMap.overallRating, 5);
      expect(fromMap.resolutionSpeed, 'Fast (Within 24 Hours)');
      expect(
        fromMap.comments,
        'Technicians were polite and tested the fan before leaving.',
      );
    });
  });

  group('AIService Tests', () {
    test(
      'AIService accurately detects Safety category and Emergency severity',
      () {
        final category = AIService.detectCategory(
          'There is a fire hazard and exposed wire near the switchboard',
        );
        expect(category, 'Safety');

        final result = AIService.analyzeIncident(
          'Fire and smoke coming from electrical outlet',
        );
        expect(result.suggestedCategory, 'Safety');
        expect(result.severity, 'Emergency');
        expect(result.confidence, greaterThan(0.6));
      },
    );

    test('AIService accurately detects Maintenance from plumbing leaks', () {
      final category = AIService.detectCategory(
        'The toilet pipe has a big leak and water is overflowing',
      );
      expect(category, 'Maintenance');
    });

    test('AIService accurately detects Cleanliness from trash and odors', () {
      final category = AIService.detectCategory(
        'Overflowing garbage trash bin causing foul smell in hallway',
      );
      expect(category, 'Cleanliness');
    });
  });
}
