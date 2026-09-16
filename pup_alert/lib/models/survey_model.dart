class PerformanceSurvey {
  final String id;
  final String reportId;
  final String reportTitle;
  final String studentUid;
  final String studentName;
  final String studentId;
  final int overallRating; // 1 to 5
  final String resolutionSpeed; // 'Fast', 'Moderate', 'Slow'
  final String staffProfessionalism; // 'Excellent', 'Good', 'Needs Improvement'
  final String
  safetyImprovement; // 'Significantly Safer', 'Somewhat Safer', 'No Change'
  final String comments;
  final int timestamp;

  PerformanceSurvey({
    this.id = '',
    required this.reportId,
    required this.reportTitle,
    required this.studentUid,
    required this.studentName,
    required this.studentId,
    this.overallRating = 5,
    this.resolutionSpeed = 'Fast',
    this.staffProfessionalism = 'Excellent',
    this.safetyImprovement = 'Significantly Safer',
    this.comments = '',
    required this.timestamp,
  });

  factory PerformanceSurvey.fromFirestore(
    Map<String, dynamic> data,
    String documentId,
  ) {
    return PerformanceSurvey(
      id: documentId,
      reportId: data['reportId'] as String? ?? '',
      reportTitle: data['reportTitle'] as String? ?? '',
      studentUid: data['studentUid'] as String? ?? '',
      studentName: data['studentName'] as String? ?? '',
      studentId: data['studentId'] as String? ?? '',
      overallRating: (data['overallRating'] as num?)?.toInt() ?? 5,
      resolutionSpeed: data['resolutionSpeed'] as String? ?? 'Fast',
      staffProfessionalism:
          data['staffProfessionalism'] as String? ?? 'Excellent',
      safetyImprovement:
          data['safetyImprovement'] as String? ?? 'Significantly Safer',
      comments: data['comments'] as String? ?? '',
      timestamp: (data['timestamp'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'reportId': reportId,
      'reportTitle': reportTitle,
      'studentUid': studentUid,
      'studentName': studentName,
      'studentId': studentId,
      'overallRating': overallRating,
      'resolutionSpeed': resolutionSpeed,
      'staffProfessionalism': staffProfessionalism,
      'safetyImprovement': safetyImprovement,
      'comments': comments,
      'timestamp': timestamp,
    };
  }
}
