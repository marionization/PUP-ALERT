import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../models/report_model.dart';
import '../../models/review_model.dart';
import '../../models/survey_model.dart';
import '../../services/auth_service.dart';
import '../../services/report_service.dart';
import '../../theme/app_theme.dart';
import '../../widgets/report_image_view.dart';

class ReportDetailScreen extends StatefulWidget {
  final Report report;
  final String userRole;

  const ReportDetailScreen({
    super.key,
    required this.report,
    required this.userRole,
  });

  @override
  State<ReportDetailScreen> createState() => _ReportDetailScreenState();
}

class _ReportDetailScreenState extends State<ReportDetailScreen> {
  final _reportService = ReportService();
  final _authService = AuthService();
  late String _currentStatus;

  @override
  void initState() {
    super.initState();
    _currentStatus = widget.report.status;

    // Trigger post-resolution survey for students if report is resolved
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _checkAndShowPerformanceSurvey();
    });
  }

  Future<void> _checkAndShowPerformanceSurvey() async {
    final isAdmin =
        widget.userRole.toLowerCase() == 'admin' ||
        widget.userRole.toLowerCase() == 'administrator';
    if (isAdmin) return;
    if (_currentStatus.toLowerCase() != 'resolved') return;

    final currentUid = _authService.currentUser?.uid ?? '';
    if (currentUid.isEmpty) return;

    final alreadySubmitted = await _reportService.hasStudentSubmittedSurvey(
      widget.report.id,
      currentUid,
    );

    if (!alreadySubmitted && mounted) {
      _showPerformanceSurveyDialog();
    }
  }

  void _showPerformanceSurveyDialog() async {
    int rating = 5;
    String speed = 'Fast (Within 24 Hours)';
    String professionalism = 'Excellent';
    String safety = 'Significantly Safer';
    final commentsController = TextEditingController();

    final session = await _authService.getCurrentSession();
    final studentName = session['name'] ?? widget.report.reporter;
    final studentId = session['studentId'] ?? widget.report.reporterStudentId;
    final studentUid = _authService.currentUser?.uid ?? '';

    if (!mounted) return;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (dialogCtx) {
        return StatefulBuilder(
          builder: (_, setModalState) {
            return AlertDialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              title: const Row(
                children: [
                  Icon(Icons.assignment_turned_in, color: AppTheme.pupMaroon),
                  SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Resolution Performance Survey',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.green.shade50,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: Colors.green.shade200),
                      ),
                      child: const Row(
                        children: [
                          Icon(
                            Icons.check_circle,
                            color: Colors.green,
                            size: 20,
                          ),
                          SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'This incident has been marked Resolved by university administrators. Help us evaluate response performance.',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.green,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Q1: Overall Resolution Satisfaction (1-5 stars)
                    const Text(
                      '1. Overall Resolution Satisfaction:',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(5, (idx) {
                        final starValue = idx + 1;
                        return IconButton(
                          icon: Icon(
                            idx < rating ? Icons.star : Icons.star_border,
                            color: AppTheme.pupGold,
                            size: 32,
                          ),
                          onPressed: () =>
                              setModalState(() => rating = starValue),
                        );
                      }),
                    ),
                    const SizedBox(height: 14),

                    // Q2: Resolution & Response Speed
                    const Text(
                      '2. University Response & Repair Speed:',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                    const SizedBox(height: 6),
                    DropdownButtonFormField<String>(
                      initialValue: speed,
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        isDense: true,
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 10,
                        ),
                      ),
                      items:
                          [
                                'Fast (Within 24 Hours)',
                                'Moderate (2-3 Days)',
                                'Slow (> 3 Days)',
                              ]
                              .map(
                                (s) => DropdownMenuItem(
                                  value: s,
                                  child: Text(
                                    s,
                                    style: const TextStyle(fontSize: 13),
                                  ),
                                ),
                              )
                              .toList(),
                      onChanged: (val) {
                        if (val != null) setModalState(() => speed = val);
                      },
                    ),
                    const SizedBox(height: 14),

                    // Q3: Staff Quality & Communication
                    const Text(
                      '3. Staff Quality & Resolution Execution:',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                    const SizedBox(height: 6),
                    DropdownButtonFormField<String>(
                      initialValue: professionalism,
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        isDense: true,
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 10,
                        ),
                      ),
                      items:
                          [
                                'Excellent',
                                'Good / Satisfactory',
                                'Needs Improvement',
                              ]
                              .map(
                                (p) => DropdownMenuItem(
                                  value: p,
                                  child: Text(
                                    p,
                                    style: const TextStyle(fontSize: 13),
                                  ),
                                ),
                              )
                              .toList(),
                      onChanged: (val) {
                        if (val != null)
                          setModalState(() => professionalism = val);
                      },
                    ),
                    const SizedBox(height: 14),

                    // Q4: Safety Impact
                    const Text(
                      '4. Does the campus area feel safe now?:',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                    const SizedBox(height: 6),
                    DropdownButtonFormField<String>(
                      initialValue: safety,
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        isDense: true,
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 10,
                        ),
                      ),
                      items:
                          [
                                'Significantly Safer',
                                'Somewhat Safer',
                                'No Change / Still Concerned',
                              ]
                              .map(
                                (v) => DropdownMenuItem(
                                  value: v,
                                  child: Text(
                                    v,
                                    style: const TextStyle(fontSize: 13),
                                  ),
                                ),
                              )
                              .toList(),
                      onChanged: (val) {
                        if (val != null) setModalState(() => safety = val);
                      },
                    ),
                    const SizedBox(height: 14),

                    // Q5: Additional Comments
                    const Text(
                      '5. Additional Comments or Staff Commendations (Optional):',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                    const SizedBox(height: 6),
                    TextField(
                      controller: commentsController,
                      maxLines: 2,
                      decoration: const InputDecoration(
                        hintText:
                            'Share any notes on the repair or safety staff...',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Later'),
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.pupMaroon,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: () async {
                    Navigator.pop(dialogCtx);
                    final survey = PerformanceSurvey(
                      reportId: widget.report.id,
                      reportTitle: widget.report.title,
                      studentUid: studentUid,
                      studentName: studentName,
                      studentId: studentId,
                      overallRating: rating,
                      resolutionSpeed: speed,
                      staffProfessionalism: professionalism,
                      safetyImprovement: safety,
                      comments: commentsController.text.trim(),
                      timestamp: DateTime.now().millisecondsSinceEpoch,
                    );
                    await _reportService.submitPerformanceSurvey(survey);
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text(
                            '🎉 Thank you for rating the campus response team! Your feedback is shared with administration.',
                          ),
                          backgroundColor: Colors.green,
                        ),
                      );
                    }
                  },
                  child: const Text('Submit Survey'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  void _showAdminReplyDialog(Review r) {
    final replyController = TextEditingController(text: r.adminReply);

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          title: Row(
            children: [
              const Icon(Icons.shield, color: AppTheme.pupMaroon),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  r.adminReply.isEmpty
                      ? 'Reply to Student Review'
                      : 'Edit Official Reply',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Review by ${r.reviewerName}:',
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 12,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '"${r.text}"',
                      style: const TextStyle(
                        fontStyle: FontStyle.italic,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              const Text(
                'University Staff Official Response:',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
              ),
              const SizedBox(height: 6),
              TextField(
                controller: replyController,
                maxLines: 3,
                decoration: const InputDecoration(
                  hintText: 'Enter official reply regarding the resolution or action taken...',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.pupMaroon,
                foregroundColor: Colors.white,
              ),
              onPressed: () async {
                final text = replyController.text.trim();
                if (text.isEmpty) return;
                Navigator.pop(ctx);

                await _reportService.replyToReview(
                  reportId: widget.report.id,
                  reviewId: r.id,
                  adminReply: text,
                  adminReplierName: 'Campus Safety Administrator',
                );

                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('✅ Official reply published successfully!'),
                      backgroundColor: Colors.green,
                    ),
                  );
                }
              },
              child: const Text('Publish Reply'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _updateStatus(String newStatus) async {
    setState(() => _currentStatus = newStatus);
    try {
      await _reportService.updateReportStatus(widget.report.id, newStatus);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Status updated to $newStatus')));
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('Failed to update status: $e')));
    }
  }

  void _showAddReviewDialog() {
    int rating = 5;
    final textController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text('Add Feedback / Rating'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Rate resolution or impact:'),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(5, (index) {
                      return IconButton(
                        icon: Icon(
                          index < rating ? Icons.star : Icons.star_border,
                          color: AppTheme.pupGold,
                          size: 32,
                        ),
                        onPressed: () =>
                            setDialogState(() => rating = index + 1),
                      );
                    }),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: textController,
                    maxLines: 3,
                    decoration: const InputDecoration(
                      hintText: 'Share feedback or work updates...',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () async {
                    final text = textController.text.trim();
                    if (text.isEmpty) return;
                    Navigator.pop(context);

                    final session = await _authService.getCurrentSession();
                    await _reportService.addReview(
                      reportId: widget.report.id,
                      rating: rating,
                      text: text,
                      reviewerName: session['name'] ?? 'PUPian',
                      reviewerStudentId: session['studentId'] ?? '',
                    );
                  },
                  child: const Text('Submit'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final isAdmin = widget.userRole.toLowerCase() == 'admin';

    return Scaffold(
      appBar: AppBar(title: const Text('Report Details')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 700),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Title and Status
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Text(
                        widget.report.title,
                        style: const TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    _buildStatusBadge(_currentStatus),
                  ],
                ),
                const SizedBox(height: 8),

                // Category & Date
                Row(
                  children: [
                    Chip(
                      label: Text(
                        widget.report.category,
                        style: const TextStyle(fontSize: 12),
                      ),
                      backgroundColor: Colors.grey.shade100,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'Submitted ${widget.report.dateSubmitted}',
                      style: const TextStyle(color: Colors.grey, fontSize: 12),
                    ),
                  ],
                ),
                const SizedBox(height: 12),

                // Location info
                Card(
                  color: Colors.grey.shade50,
                  margin: EdgeInsets.zero,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 8,
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.location_on,
                          color: AppTheme.pupMaroon,
                          size: 20,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            widget.report.location,
                            style: const TextStyle(fontWeight: FontWeight.w600),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Image / Media if available
                if (widget.report.hasImage) ...[
                  Row(
                    children: [
                      const Icon(
                        Icons.photo_camera_back_outlined,
                        size: 18,
                        color: AppTheme.pupMaroon,
                      ),
                      const SizedBox(width: 8),
                      const Text(
                        'Attached Incident Photo',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 15,
                        ),
                      ),
                      const Spacer(),
                      Row(
                        children: [
                          Icon(
                            Icons.zoom_in,
                            size: 16,
                            color: Colors.grey.shade600,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            'Tap to enlarge',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey.shade600,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.08),
                          blurRadius: 10,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: ReportImageView(
                      imageUrl: widget.report.displayImageUrl,
                      height: 280,
                      width: double.infinity,
                      fit: BoxFit.cover,
                      borderRadius: BorderRadius.circular(12),
                      enableZoom: true,
                    ),
                  ),
                  const SizedBox(height: 18),
                ],

                // Description
                const Text(
                  'Description',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
                const SizedBox(height: 6),
                Text(
                  widget.report.description,
                  style: const TextStyle(fontSize: 14, height: 1.4),
                ),
                const SizedBox(height: 12),

                // Reporter Info
                Text(
                  'Reported by: ${widget.report.isAnonymous ? "Anonymous Student" : widget.report.reporter}',
                  style: const TextStyle(
                    fontStyle: FontStyle.italic,
                    color: Colors.grey,
                    fontSize: 13,
                  ),
                ),
                const SizedBox(height: 20),

                // Admin Status Update Section
                if (isAdmin) ...[
                  Card(
                    color: AppTheme.pupLightGold,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            children: [
                              Icon(
                                Icons.admin_panel_settings,
                                color: AppTheme.pupMaroon,
                              ),
                              SizedBox(width: 8),
                              Text(
                                'Admin Controls: Update Status',
                                style: TextStyle(fontWeight: FontWeight.bold),
                              ),
                            ],
                          ),
                          const SizedBox(height: 10),
                          DropdownButtonFormField<String>(
                            initialValue: _currentStatus,
                            decoration: const InputDecoration(
                              fillColor: Colors.white,
                              filled: true,
                              border: OutlineInputBorder(),
                            ),
                            items: const [
                              DropdownMenuItem(
                                value: 'In Review',
                                child: Text('In Review'),
                              ),
                              DropdownMenuItem(
                                value: 'In Progress',
                                child: Text('In Progress'),
                              ),
                              DropdownMenuItem(
                                value: 'Resolved',
                                child: Text('Resolved'),
                              ),
                            ],
                            onChanged: (newStatus) {
                              if (newStatus != null) _updateStatus(newStatus);
                            },
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                ],

                const Divider(),
                const SizedBox(height: 10),

                // Feedback & Reviews Header
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Feedback & Reviews',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                      ),
                    ),
                    if (!isAdmin)
                      ElevatedButton.icon(
                        onPressed: _showAddReviewDialog,
                        icon: const Icon(Icons.rate_review, size: 16),
                        label: const Text('Add Review'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.pupMaroon,
                          foregroundColor: Colors.white,
                        ),
                      )
                    else
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: Colors.blue.shade50,
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(color: Colors.blue.shade200),
                        ),
                        child: const Row(
                          children: [
                            Icon(Icons.shield, size: 14, color: Colors.blue),
                            SizedBox(width: 4),
                            Text(
                              'Admin: Reply Enabled',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                                color: Colors.blue,
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 12),

                // Stream of reviews
                StreamBuilder<List<Review>>(
                  stream: _reportService.getReviewsStream(widget.report.id),
                  builder: (context, snapshot) {
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return const Center(child: CircularProgressIndicator());
                    }
                    final reviews = snapshot.data ?? [];
                    if (reviews.isEmpty) {
                      return const Padding(
                        padding: EdgeInsets.symmetric(vertical: 16),
                        child: Text(
                          'No feedback submitted yet. Students can review resolved incidents!',
                        ),
                      );
                    }

                    return ListView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: reviews.length,
                      itemBuilder: (context, index) {
                        final r = reviews[index];
                        return Card(
                          margin: const EdgeInsets.symmetric(vertical: 6),
                          elevation: 1.5,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10),
                            side: BorderSide(color: Colors.grey.shade200),
                          ),
                          child: Padding(
                            padding: const EdgeInsets.all(14),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    CircleAvatar(
                                      radius: 14,
                                      backgroundColor: AppTheme.pupMaroon
                                          .withValues(alpha: 0.1),
                                      child: const Icon(
                                        Icons.person,
                                        size: 16,
                                        color: AppTheme.pupMaroon,
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Text(
                                      r.reviewerName,
                                      style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    const Spacer(),
                                    Row(
                                      children: List.generate(
                                        r.rating,
                                        (i) => const Icon(
                                          Icons.star,
                                          size: 14,
                                          color: AppTheme.pupGold,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                Text(
                                  r.text,
                                  style: const TextStyle(
                                    fontSize: 13,
                                    height: 1.3,
                                  ),
                                ),
                                const SizedBox(height: 8),

                                // Student feedback like/dislike counts
                                Row(
                                  children: [
                                    IconButton(
                                      icon: const Icon(
                                        Icons.thumb_up_outlined,
                                        size: 16,
                                      ),
                                      onPressed: () =>
                                          _reportService.toggleLikeReview(
                                            widget.report.id,
                                            r.id,
                                            true,
                                          ),
                                    ),
                                    Text(
                                      '${r.likes}',
                                      style: const TextStyle(fontSize: 12),
                                    ),
                                    const SizedBox(width: 8),
                                    IconButton(
                                      icon: const Icon(
                                        Icons.thumb_down_outlined,
                                        size: 16,
                                      ),
                                      onPressed: () =>
                                          _reportService.toggleLikeReview(
                                            widget.report.id,
                                            r.id,
                                            false,
                                          ),
                                    ),
                                    Text(
                                      '${r.dislikes}',
                                      style: const TextStyle(fontSize: 12),
                                    ),
                                    const Spacer(),
                                    if (isAdmin)
                                      TextButton.icon(
                                        style: TextButton.styleFrom(
                                          foregroundColor: AppTheme.pupMaroon,
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 8,
                                            vertical: 4,
                                          ),
                                        ),
                                        icon: Icon(
                                          r.adminReply.isEmpty
                                              ? Icons.reply
                                              : Icons.edit,
                                          size: 14,
                                        ),
                                        label: Text(
                                          r.adminReply.isEmpty
                                              ? 'Reply as Admin'
                                              : 'Edit Reply',
                                          style: const TextStyle(
                                            fontSize: 12,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                        onPressed: () =>
                                            _showAdminReplyDialog(r),
                                      ),
                                  ],
                                ),

                                // Official University Staff Reply Bubble
                                if (r.adminReply.isNotEmpty) ...[
                                  const SizedBox(height: 8),
                                  Container(
                                    width: double.infinity,
                                    padding: const EdgeInsets.all(12),
                                    decoration: BoxDecoration(
                                      color: const Color(0xFF800000)
                                          .withValues(alpha: 0.05),
                                      borderRadius: BorderRadius.circular(8),
                                      border: Border.all(
                                        color: const Color(0xFF800000)
                                            .withValues(alpha: 0.2),
                                      ),
                                    ),
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Row(
                                          children: [
                                            const Icon(
                                              Icons.verified,
                                              size: 15,
                                              color: Color(0xFF800000),
                                            ),
                                            const SizedBox(width: 6),
                                            Text(
                                              r.adminReplierName.isNotEmpty
                                                  ? r.adminReplierName
                                                  : 'University Staff Official Response',
                                              style: const TextStyle(
                                                fontWeight: FontWeight.bold,
                                                fontSize: 12,
                                                color: Color(0xFF800000),
                                              ),
                                            ),
                                            const Spacer(),
                                            if (r.adminRepliedAt > 0)
                                              Text(
                                                DateFormat(
                                                  'MMM d, yyyy',
                                                ).format(
                                                  DateTime.fromMillisecondsSinceEpoch(
                                                    r.adminRepliedAt,
                                                  ),
                                                ),
                                                style: TextStyle(
                                                  fontSize: 10,
                                                  color: Colors.grey.shade600,
                                                ),
                                              ),
                                          ],
                                        ),
                                        const SizedBox(height: 6),
                                        Text(
                                          r.adminReply,
                                          style: const TextStyle(
                                            fontSize: 13,
                                            height: 1.35,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ),
                        );
                      },
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildStatusBadge(String status) {
    Color bg;
    Color fg;
    switch (status.toLowerCase()) {
      case 'in progress':
        bg = Colors.blue.shade100;
        fg = Colors.blue.shade800;
        break;
      case 'resolved':
        bg = Colors.green.shade100;
        fg = Colors.green.shade800;
        break;
      case 'in review':
      default:
        bg = Colors.orange.shade100;
        fg = Colors.orange.shade900;
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        status,
        style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: fg),
      ),
    );
  }
}
