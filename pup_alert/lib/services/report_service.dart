import 'dart:convert';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter/foundation.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import '../models/report_model.dart';
import '../models/review_model.dart';
import '../models/survey_model.dart';

class ReportService {
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  final FirebaseStorage _storage = FirebaseStorage.instance;

  CollectionReference get _reportsRef => _firestore.collection('reports');

  // Real-time stream of all reports ordered by timestamp descending
  Stream<List<Report>> getReportsStream() {
    return _reportsRef.snapshots().map((snapshot) {
      final reports = <Report>[];
      for (final doc in snapshot.docs) {
        try {
          final data = doc.data() as Map<String, dynamic>?;
          if (data != null) {
            reports.add(Report.fromFirestore(data, doc.id));
          }
        } catch (e) {
          // Ignore individual malformed doc and continue
        }
      }
      // Sort newest first by timestamp
      reports.sort((a, b) => b.timestamp.compareTo(a.timestamp));
      return reports;
    });
  }

  // Real-time stream of reviews for a report
  Stream<List<Review>> getReviewsStream(String reportId) {
    return _reportsRef.doc(reportId).collection('reviews').snapshots().map((
      snapshot,
    ) {
      return snapshot.docs.map((doc) {
        return Review.fromFirestore(doc.data(), doc.id);
      }).toList();
    });
  }

  // Submit a new report with optional media upload
  Future<String> submitReport({
    required String title,
    required String category,
    required String location,
    required String description,
    XFile? mediaFile,
    bool isVideo = false,
    required bool isAnonymous,
    required String reporterName,
    required String reporterUid,
    required String reporterStudentId,
  }) async {
    String mediaUrl = '';
    String base64Fallback = '';
    String mediaType = isVideo ? 'video' : 'image';

    if (mediaFile != null) {
      try {
        final bytes = await mediaFile.readAsBytes();

        // Prepare base64 fallback so picture is 100% visible even if Storage has CORS/rules issues
        if (!isVideo && bytes.lengthInBytes <= 850 * 1024) {
          final isPng = mediaFile.name.toLowerCase().endsWith('.png');
          final mime = isPng ? 'image/png' : 'image/jpeg';
          base64Fallback = 'data:$mime;base64,${base64Encode(bytes)}';
        }

        try {
          final extension = mediaFile.name.split('.').last.toLowerCase();
          final fileName =
              '${DateTime.now().millisecondsSinceEpoch}_$reporterUid.$extension';
          final folder = isVideo ? 'report_videos' : 'report_images';
          final ref = _storage.ref().child('$folder/$fileName');

          final uploadTask = await ref.putData(
            bytes,
            SettableMetadata(
              contentType: isVideo ? 'video/$extension' : 'image/$extension',
            ),
          );
          mediaUrl = await uploadTask.ref.getDownloadURL();
        } catch (storageError) {
          debugPrint(
            'Firebase Storage upload encountered an issue, using base64 data URL: $storageError',
          );
          if (base64Fallback.isNotEmpty) {
            mediaUrl = base64Fallback;
          }
        }

        // If Storage returned empty, ensure fallback is utilized
        if (mediaUrl.isEmpty && base64Fallback.isNotEmpty) {
          mediaUrl = base64Fallback;
        }
      } catch (e) {
        debugPrint('Media processing error: $e');
      }
    }

    final dateFormatted = DateFormat('MMMM d, yyyy').format(DateTime.now());
    final docRef = _reportsRef.doc();

    final reportData = {
      'reportId': docRef.id,
      'title': title,
      'category': category,
      'location': location,
      'description': description,
      'imageUrl': base64Fallback.isNotEmpty ? base64Fallback : mediaUrl,
      'mediaUrl': mediaUrl,
      'mediaType': mediaType,
      'status': 'In Review',
      'dateSubmitted': dateFormatted,
      'reporter': isAnonymous ? 'Anonymous Student' : reporterName,
      'timestamp': Timestamp.now(),
      'averageRating': 0.0,
      'ratingCount': 0,
      'isAnonymous': isAnonymous,
      'reporterUid': reporterUid,
      'reporterStudentId': isAnonymous ? '' : reporterStudentId,
    };

    await docRef.set(reportData);

    // Create Admin Notification matching SubmitReportActivity.kt line 1208
    try {
      await _firestore.collection('notifications').add({
        'reportId': docRef.id,
        'title': 'New Report Submitted',
        'message': isAnonymous
            ? 'A new report "$title" was submitted by Anonymous Student.'
            : 'A new report "$title" was submitted by $reporterName.',
        'reportTitle': title,
        'status': 'In Review',
        'reporter': isAnonymous ? 'Anonymous Student' : reporterName,
        'isAnonymous': isAnonymous,
        'targetRole': 'Admin',
        'targetUser': '',
        'type': 'new_report',
        'read': false,
        'timestamp': Timestamp.now(),
      });
    } catch (_) {}

    return docRef.id;
  }

  // Update report status (Admin feature)
  Future<void> updateReportStatus(String reportId, String newStatus) async {
    await _reportsRef.doc(reportId).update({'status': newStatus});
  }

  // Add review and update average rating
  Future<void> addReview({
    required String reportId,
    required int rating,
    required String text,
    required String reviewerName,
    required String reviewerStudentId,
  }) async {
    final reviewRef = _reportsRef.doc(reportId).collection('reviews').doc();
    final review = Review(
      id: reviewRef.id,
      rating: rating,
      text: text,
      reviewerName: reviewerName,
      reviewerStudentId: reviewerStudentId,
      likes: 0,
      dislikes: 0,
      timestamp: DateTime.now().millisecondsSinceEpoch,
    );

    await reviewRef.set(review.toFirestore());

    // Recompute average rating for the report
    final reviewsSnapshot = await _reportsRef
        .doc(reportId)
        .collection('reviews')
        .get();
    if (reviewsSnapshot.docs.isNotEmpty) {
      double totalRating = 0;
      for (var doc in reviewsSnapshot.docs) {
        totalRating += (doc.data()['rating'] as num?)?.toDouble() ?? 5.0;
      }
      final count = reviewsSnapshot.docs.length;
      final avg = totalRating / count;
      await _reportsRef.doc(reportId).update({
        'averageRating': double.parse(avg.toStringAsFixed(1)),
        'ratingCount': count,
      });
    }
  }

  // Admin replies to a student review
  Future<void> replyToReview({
    required String reportId,
    required String reviewId,
    required String adminReply,
    required String adminReplierName,
  }) async {
    await _reportsRef.doc(reportId).collection('reviews').doc(reviewId).update({
      'adminReply': adminReply,
      'adminRepliedAt': DateTime.now().millisecondsSinceEpoch,
      'adminReplierName': adminReplierName,
    });
  }

  // Upvote / Like a review
  Future<void> toggleLikeReview(
    String reportId,
    String reviewId,
    bool isLike,
  ) async {
    final field = isLike ? 'likes' : 'dislikes';
    await _reportsRef.doc(reportId).collection('reviews').doc(reviewId).update({
      field: FieldValue.increment(1),
    });
  }

  // Check if student has already completed performance survey for this resolved report
  Future<bool> hasStudentSubmittedSurvey(
    String reportId,
    String studentUid,
  ) async {
    if (reportId.isEmpty || studentUid.isEmpty) return false;
    try {
      final snapshot = await _firestore
          .collection('performance_surveys')
          .where('reportId', isEqualTo: reportId)
          .where('studentUid', isEqualTo: studentUid)
          .limit(1)
          .get();
      return snapshot.docs.isNotEmpty;
    } catch (_) {
      return false;
    }
  }

  // Submit a post-resolution performance survey
  Future<void> submitPerformanceSurvey(PerformanceSurvey survey) async {
    final docRef = _firestore.collection('performance_surveys').doc();
    await docRef.set(survey.toFirestore());
  }

  // Real-time stream of all performance surveys for admin analytics
  Stream<List<PerformanceSurvey>> getPerformanceSurveysStream() {
    return _firestore.collection('performance_surveys').snapshots().map((
      snapshot,
    ) {
      final surveys = <PerformanceSurvey>[];
      for (var doc in snapshot.docs) {
        try {
          final data = doc.data();
          surveys.add(PerformanceSurvey.fromFirestore(data, doc.id));
        } catch (_) {}
      }
      surveys.sort((a, b) => b.timestamp.compareTo(a.timestamp));
      return surveys;
    });
  }
}
