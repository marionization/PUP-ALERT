import 'package:cloud_firestore/cloud_firestore.dart';

class Report {
  final String id;
  final String title;
  final String category;
  final String location;
  final String description;
  final String? imageUrl;
  final String mediaUrl;
  final String mediaType;
  final String status;
  final String dateSubmitted;
  final String reporter;
  final int timestamp;
  final double averageRating;
  final int ratingCount;
  final bool isAnonymous;
  final String reporterUid;
  final String reporterStudentId;

  /// Returns true if the report has an attached photo/video URL or base64 data
  bool get hasImage {
    final effective = mediaUrl.trim().isNotEmpty
        ? mediaUrl.trim()
        : (imageUrl?.trim() ?? '');
    return effective.isNotEmpty;
  }

  /// Returns the effective photo/media URL or base64 data URI
  String get displayImageUrl {
    final cleanImage = imageUrl?.trim() ?? '';
    final cleanMedia = mediaUrl.trim();

    // Prefer base64 data URI if either has it (immune to CORS, renders instantly)
    if (cleanImage.startsWith('data:image') ||
        cleanImage.startsWith('data:application')) {
      return cleanImage;
    }
    if (cleanMedia.startsWith('data:image') ||
        cleanMedia.startsWith('data:application')) {
      return cleanMedia;
    }
    if (cleanMedia.isNotEmpty) return cleanMedia;
    if (cleanImage.isNotEmpty) return cleanImage;
    return '';
  }

  Report({
    this.id = '',
    this.title = '',
    this.category = '',
    this.location = '',
    this.description = '',
    this.imageUrl,
    this.mediaUrl = '',
    this.mediaType = '',
    this.status = 'In Review',
    this.dateSubmitted = '',
    this.reporter = '',
    this.timestamp = 0,
    this.averageRating = 0.0,
    this.ratingCount = 0,
    this.isAnonymous = false,
    this.reporterUid = '',
    this.reporterStudentId = '',
  });

  factory Report.fromDoc(DocumentSnapshot doc) {
    return Report.fromFirestore(
      doc.data() as Map<String, dynamic>? ?? {},
      doc.id,
    );
  }

  factory Report.fromFirestore(Map<String, dynamic> data, String documentId) {
    int parsedTimestamp = 0;
    final rawTs = data['timestamp'];
    if (rawTs is Timestamp) {
      parsedTimestamp = rawTs.millisecondsSinceEpoch;
    } else if (rawTs is num) {
      parsedTimestamp = rawTs.toInt();
    } else if (rawTs is String) {
      parsedTimestamp = int.tryParse(rawTs) ?? 0;
    }

    double parsedRating = 0.0;
    final rawRating = data['averageRating'];
    if (rawRating is num) {
      parsedRating = rawRating.toDouble();
    } else if (rawRating is String) {
      parsedRating = double.tryParse(rawRating) ?? 0.0;
    }

    int parsedCount = 0;
    final rawCount = data['ratingCount'];
    if (rawCount is num) {
      parsedCount = rawCount.toInt();
    } else if (rawCount is String) {
      parsedCount = int.tryParse(rawCount) ?? 0;
    }

    bool parsedAnon = false;
    final rawAnon = data['isAnonymous'];
    if (rawAnon is bool) {
      parsedAnon = rawAnon;
    } else if (rawAnon is String) {
      parsedAnon = rawAnon.toLowerCase() == 'true';
    }

    final rawMedia = data['mediaUrl']?.toString().trim() ?? '';
    final rawImage = data['imageUrl']?.toString().trim() ?? '';

    return Report(
      id: documentId,
      title: data['title']?.toString() ?? '',
      category: data['category']?.toString() ?? 'Other',
      location: data['location']?.toString() ?? '',
      description: data['description']?.toString() ?? '',
      imageUrl: rawImage.isNotEmpty ? rawImage : rawMedia,
      mediaUrl: rawMedia.isNotEmpty ? rawMedia : rawImage,
      mediaType: data['mediaType']?.toString() ?? 'image',
      status: data['status']?.toString() ?? 'In Review',
      dateSubmitted: data['dateSubmitted']?.toString() ?? '',
      reporter: data['reporter']?.toString() ?? '',
      timestamp: parsedTimestamp,
      averageRating: parsedRating,
      ratingCount: parsedCount,
      isAnonymous: parsedAnon,
      reporterUid: data['reporterUid']?.toString() ?? '',
      reporterStudentId: data['reporterStudentId']?.toString() ?? '',
    );
  }

  Map<String, dynamic> toFirestore() {
    final effectiveMedia = mediaUrl.trim().isNotEmpty
        ? mediaUrl.trim()
        : (imageUrl?.trim() ?? '');
    final effectiveImage = (imageUrl != null && imageUrl!.trim().isNotEmpty)
        ? imageUrl!.trim()
        : effectiveMedia;
    return {
      'title': title,
      'category': category,
      'location': location,
      'description': description,
      'imageUrl': effectiveImage,
      'mediaUrl': effectiveMedia,
      'mediaType': mediaType,
      'status': status,
      'dateSubmitted': dateSubmitted,
      'reporter': reporter,
      'timestamp': timestamp,
      'averageRating': averageRating,
      'ratingCount': ratingCount,
      'isAnonymous': isAnonymous,
      'reporterUid': reporterUid,
      'reporterStudentId': reporterStudentId,
    };
  }

  Report copyWith({
    String? id,
    String? title,
    String? category,
    String? location,
    String? description,
    String? imageUrl,
    String? mediaUrl,
    String? mediaType,
    String? status,
    String? dateSubmitted,
    String? reporter,
    int? timestamp,
    double? averageRating,
    int? ratingCount,
    bool? isAnonymous,
    String? reporterUid,
    String? reporterStudentId,
  }) {
    return Report(
      id: id ?? this.id,
      title: title ?? this.title,
      category: category ?? this.category,
      location: location ?? this.location,
      description: description ?? this.description,
      imageUrl: imageUrl ?? this.imageUrl,
      mediaUrl: mediaUrl ?? this.mediaUrl,
      mediaType: mediaType ?? this.mediaType,
      status: status ?? this.status,
      dateSubmitted: dateSubmitted ?? this.dateSubmitted,
      reporter: reporter ?? this.reporter,
      timestamp: timestamp ?? this.timestamp,
      averageRating: averageRating ?? this.averageRating,
      ratingCount: ratingCount ?? this.ratingCount,
      isAnonymous: isAnonymous ?? this.isAnonymous,
      reporterUid: reporterUid ?? this.reporterUid,
      reporterStudentId: reporterStudentId ?? this.reporterStudentId,
    );
  }
}
