import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../models/accessibility_settings.dart';
import '../../models/report_model.dart';
import '../../models/survey_model.dart';
import '../../services/analytics_service.dart';
import '../../services/report_service.dart';
import '../../theme/app_theme.dart';

class AnalyticsScreen extends StatefulWidget {
  final bool isEmbedded;
  final AccessibilitySettings? settings;
  final String userRole;

  const AnalyticsScreen({
    super.key,
    this.isEmbedded = false,
    this.settings,
    this.userRole = 'admin',
  });

  @override
  State<AnalyticsScreen> createState() => _AnalyticsScreenState();
}

class _AnalyticsScreenState extends State<AnalyticsScreen> {
  final _reportService = ReportService();
  bool _isExporting = false;

  void _showSurveysPopup(
    BuildContext context,
    List<PerformanceSurvey> surveys,
    AccessibilitySettings settings,
  ) {
    showDialog(
      context: context,
      builder: (ctx) {
        return Dialog(
          backgroundColor: settings.cardColor,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: BorderSide(color: settings.borderColor),
          ),
          child: Container(
            width: 650,
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              color: settings.cardColor,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: settings.accentColor.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Icon(
                        Icons.rate_review,
                        color: settings.accentColor,
                        size: 22,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Student Performance Survey Answers (${surveys.length})',
                            style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: settings.primaryTextColor,
                            ),
                          ),
                          Text(
                            'Evaluations submitted by students after report resolution',
                            style: TextStyle(
                              fontSize: 12,
                              color: settings.secondaryTextColor,
                            ),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: Icon(Icons.close, color: settings.primaryTextColor),
                      onPressed: () => Navigator.pop(ctx),
                    ),
                  ],
                ),
                Divider(color: settings.borderColor),
                if (surveys.isEmpty)
                  Padding(
                    padding: const EdgeInsets.all(32),
                    child: Center(
                      child: Column(
                        children: [
                          Icon(
                            Icons.assignment_outlined,
                            size: 48,
                            color: settings.secondaryTextColor,
                          ),
                          const SizedBox(height: 10),
                          Text(
                            'No performance surveys submitted yet.',
                            style: TextStyle(
                              color: settings.secondaryTextColor,
                            ),
                          ),
                        ],
                      ),
                    ),
                  )
                else
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxHeight: 480),
                    child: ListView.separated(
                      shrinkWrap: true,
                      itemCount: surveys.length,
                      separatorBuilder: (_, _) => Divider(
                        height: 1,
                        color: settings.borderColor.withValues(alpha: 0.5),
                      ),
                      itemBuilder: (context, i) {
                        final s = surveys[i];
                        return Padding(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  CircleAvatar(
                                    radius: 14,
                                    backgroundColor: settings.accentColor
                                        .withValues(alpha: 0.12),
                                    child: Icon(
                                      Icons.person,
                                      size: 16,
                                      color: settings.accentColor,
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    s.studentName.isNotEmpty
                                        ? s.studentName
                                        : 'Student',
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      color: settings.primaryTextColor,
                                    ),
                                  ),
                                  if (s.studentId.isNotEmpty) ...[
                                    const SizedBox(width: 6),
                                    Text(
                                      '(${s.studentId})',
                                      style: TextStyle(
                                        fontSize: 11,
                                        color: settings.secondaryTextColor,
                                      ),
                                    ),
                                  ],
                                  const Spacer(),
                                  Row(
                                    children: List.generate(
                                      s.overallRating,
                                      (_) => const Icon(
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
                                'Report: "${s.reportTitle}"',
                                style: TextStyle(
                                  fontWeight: FontWeight.w600,
                                  fontSize: 13,
                                  color: settings.accentColor,
                                ),
                              ),
                              const SizedBox(height: 6),
                              Wrap(
                                spacing: 6,
                                runSpacing: 6,
                                children: [
                                  _buildTag(
                                    '⚡ Speed: ${s.resolutionSpeed}',
                                    Colors.blue,
                                  ),
                                  _buildTag(
                                    '👔 Staff: ${s.staffProfessionalism}',
                                    Colors.purple,
                                  ),
                                  _buildTag(
                                    '🛡️ Safety: ${s.safetyImprovement}',
                                    Colors.green,
                                  ),
                                ],
                              ),
                              if (s.comments.isNotEmpty) ...[
                                const SizedBox(height: 6),
                                Container(
                                  width: double.infinity,
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color:
                                        settings.contrastMode == 'Dark Contrast'
                                        ? const Color(0xFF2A2A2A)
                                        : Colors.grey.shade100,
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    '"${s.comments}"',
                                    style: TextStyle(
                                      fontStyle: FontStyle.italic,
                                      fontSize: 12,
                                      color: settings.primaryTextColor,
                                    ),
                                  ),
                                ),
                              ],
                              if (s.timestamp > 0) ...[
                                const SizedBox(height: 4),
                                Text(
                                  DateFormat('MMMM d, yyyy - h:mm a').format(
                                    DateTime.fromMillisecondsSinceEpoch(
                                      s.timestamp,
                                    ),
                                  ),
                                  style: TextStyle(
                                    fontSize: 10,
                                    color: settings.secondaryTextColor,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        );
                      },
                    ),
                  ),
                const SizedBox(height: 12),
                Align(
                  alignment: Alignment.centerRight,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: settings.accentColor,
                      foregroundColor: Colors.white,
                    ),
                    onPressed: () => Navigator.pop(ctx),
                    child: const Text('Close'),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildTag(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }

  Future<void> _handleCsvDownload(List<Report> reports) async {
    setState(() => _isExporting = true);
    try {
      final fileName = await AnalyticsService.downloadCsv(reports: reports);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('📥 Exported $fileName successfully!'),
          backgroundColor: Colors.green,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Export failed: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) setState(() => _isExporting = false);
    }
  }

  Future<void> _handlePrintPdf(List<Report> reports) async {
    setState(() => _isExporting = true);
    try {
      await AnalyticsService.printPdfReport(reports: reports);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Print preview failed: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) setState(() => _isExporting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final settings = widget.settings ?? const AccessibilitySettings();
    final isAdmin =
        widget.userRole.toLowerCase() == 'admin' ||
        widget.userRole.toLowerCase() == 'administrator';

    if (!isAdmin) {
      final accessDeniedContent = Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 480),
            child: Card(
              color: settings.cardColor,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(settings.cardBorderRadius),
                side: BorderSide(color: settings.borderColor),
              ),
              child: Padding(
                padding: const EdgeInsets.all(32),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.admin_panel_settings_outlined,
                      size: 64,
                      color: settings.accentColor,
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'Admin Exclusive Feature',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: settings.titleFontSize + 2,
                        fontWeight: settings.titleFontWeight,
                        color: settings.accentColor,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Text(
                      'Campus Safety Analytics, CSV/Excel data exports, and incident statistics are strictly reserved for university administrators and staff.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: settings.bodyFontSize,
                        color: settings.secondaryTextColor,
                        height: 1.4,
                      ),
                    ),
                    const SizedBox(height: 24),
                    if (!widget.isEmbedded)
                      ElevatedButton.icon(
                        onPressed: () => Navigator.pop(context),
                        icon: const Icon(Icons.arrow_back),
                        label: const Text('Back to App'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: settings.accentColor,
                          foregroundColor: Colors.white,
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ),
      );

      if (widget.isEmbedded) {
        return accessDeniedContent;
      }
      return Scaffold(
        backgroundColor: settings.backgroundColor,
        appBar: AppBar(
          title: const Text('Access Denied'),
          backgroundColor: settings.headerColor,
        ),
        body: accessDeniedContent,
      );
    }

    final bodyContent = StreamBuilder<List<Report>>(
      stream: _reportService.getReportsStream(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }

        final reports = snapshot.data ?? [];
        final total = reports.length;
        final inReview = reports.where((r) => r.status == 'In Review').length;
        final inProgress = reports
            .where((r) => r.status == 'In Progress')
            .length;
        final resolved = reports.where((r) => r.status == 'Resolved').length;

        return SingleChildScrollView(
          padding: EdgeInsets.all(settings.cardPadding),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 800),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Export Actions Bar
                  Card(
                    color: AppTheme.pupLightGold,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          const Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Export Campus Incident Data',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                  ),
                                ),
                                Text(
                                  'Download data for administrative reviews, inspections, and reports',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.black87,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: [
                              ElevatedButton.icon(
                                onPressed: _isExporting
                                    ? null
                                    : () => _handleCsvDownload(reports),
                                icon: const Icon(Icons.file_download, size: 18),
                                label: const Text('Excel CSV'),
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: AppTheme.pupMaroon,
                                  foregroundColor: Colors.white,
                                ),
                              ),
                              ElevatedButton.icon(
                                onPressed: _isExporting
                                    ? null
                                    : () => _handlePrintPdf(reports),
                                icon: const Icon(Icons.print, size: 18),
                                label: const Text('Print PDF'),
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: Colors.blueGrey.shade800,
                                  foregroundColor: Colors.white,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Status Stats Grid
                  LayoutBuilder(
                    builder: (context, constraints) {
                      return GridView.count(
                        crossAxisCount: constraints.maxWidth > 550 ? 4 : 2,
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        crossAxisSpacing: 12,
                        mainAxisSpacing: 12,
                        childAspectRatio: 1.3,
                        children: [
                          _buildStatCard(
                            'Total Incidents',
                            '$total',
                            Icons.analytics_outlined,
                            AppTheme.pupMaroon,
                          ),
                          _buildStatCard(
                            'In Review',
                            '$inReview',
                            Icons.pending_actions,
                            Colors.orange,
                          ),
                          _buildStatCard(
                            'In Progress',
                            '$inProgress',
                            Icons.engineering,
                            Colors.blue,
                          ),
                          _buildStatCard(
                            'Resolved',
                            '$resolved',
                            Icons.check_circle_outline,
                            Colors.green,
                          ),
                        ],
                      );
                    },
                  ),
                  const SizedBox(height: 24),

                  // Category Breakdown Chart Card
                  const Text(
                    'Reports by Category Breakdown',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Card(
                    elevation: 2,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: _buildCategoryBreakdown(reports),
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Student Performance Surveys Section (Live)
                  StreamBuilder<List<PerformanceSurvey>>(
                    stream: _reportService.getPerformanceSurveysStream(),
                    builder: (context, surveySnap) {
                      final surveys = surveySnap.data ?? [];
                      final surveyCount = surveys.length;
                      double avgRating = 0;
                      int fastCount = 0;
                      if (surveyCount > 0) {
                        final totalRating = surveys.fold<int>(
                          0,
                          (sum, s) => sum + s.overallRating,
                        );
                        avgRating = totalRating / surveyCount;
                        fastCount = surveys
                            .where((s) => s.resolutionSpeed.contains('Fast'))
                            .length;
                      }
                      final fastPct = surveyCount > 0
                          ? (fastCount / surveyCount * 100).toStringAsFixed(0)
                          : '0';

                      return Card(
                        color: settings.cardColor,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(
                            settings.cardBorderRadius,
                          ),
                          side: BorderSide(color: settings.borderColor),
                        ),
                        elevation: 2,
                        child: Padding(
                          padding: const EdgeInsets.all(18),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Container(
                                    padding: const EdgeInsets.all(8),
                                    decoration: BoxDecoration(
                                      color: settings.accentColor.withValues(
                                        alpha: 0.12,
                                      ),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: Icon(
                                      Icons.rate_review,
                                      color: settings.accentColor,
                                      size: 22,
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          'Incident Resolution & Performance Surveys',
                                          style: TextStyle(
                                            fontSize: 16,
                                            fontWeight:
                                                settings.titleFontWeight,
                                            color: settings.primaryTextColor,
                                          ),
                                        ),
                                        Text(
                                          'Evaluations collected from students upon incident resolution',
                                          style: TextStyle(
                                            fontSize: 12,
                                            color: settings.secondaryTextColor,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  ElevatedButton.icon(
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: settings.accentColor,
                                      foregroundColor: Colors.white,
                                    ),
                                    icon: const Icon(
                                      Icons.open_in_new,
                                      size: 16,
                                    ),
                                    label: Text('View Answers ($surveyCount)'),
                                    onPressed: () => _showSurveysPopup(
                                      context,
                                      surveys,
                                      settings,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 16),
                              Row(
                                children: [
                                  Expanded(
                                    child: Container(
                                      padding: const EdgeInsets.all(12),
                                      decoration: BoxDecoration(
                                        color: Colors.amber.withValues(
                                          alpha: 0.1,
                                        ),
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Column(
                                        children: [
                                          Row(
                                            mainAxisAlignment:
                                                MainAxisAlignment.center,
                                            children: [
                                              const Icon(
                                                Icons.star,
                                                color: Colors.amber,
                                                size: 18,
                                              ),
                                              const SizedBox(width: 4),
                                              Text(
                                                surveyCount > 0
                                                    ? avgRating.toStringAsFixed(
                                                        1,
                                                      )
                                                    : 'N/A',
                                                style: const TextStyle(
                                                  fontSize: 18,
                                                  fontWeight: FontWeight.bold,
                                                ),
                                              ),
                                            ],
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            'Avg Student Rating',
                                            style: TextStyle(
                                              fontSize: 11,
                                              color:
                                                  settings.secondaryTextColor,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Container(
                                      padding: const EdgeInsets.all(12),
                                      decoration: BoxDecoration(
                                        color: Colors.blue.withValues(
                                          alpha: 0.1,
                                        ),
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Column(
                                        children: [
                                          Text(
                                            '$fastPct%',
                                            style: const TextStyle(
                                              fontSize: 18,
                                              fontWeight: FontWeight.bold,
                                              color: Colors.blue,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            'Fast Resolution Rate',
                                            style: TextStyle(
                                              fontSize: 11,
                                              color:
                                                  settings.secondaryTextColor,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Container(
                                      padding: const EdgeInsets.all(12),
                                      decoration: BoxDecoration(
                                        color: Colors.green.withValues(
                                          alpha: 0.1,
                                        ),
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Column(
                                        children: [
                                          Text(
                                            '$surveyCount',
                                            style: const TextStyle(
                                              fontSize: 18,
                                              fontWeight: FontWeight.bold,
                                              color: Colors.green,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            'Total Responses',
                                            style: TextStyle(
                                              fontSize: 11,
                                              color:
                                                  settings.secondaryTextColor,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );

    if (widget.isEmbedded) {
      return bodyContent;
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Campus Analytics & Reports')),
      body: bodyContent,
    );
  }

  Widget _buildStatCard(
    String label,
    String value,
    IconData icon,
    Color color,
  ) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: color, size: 28),
            const SizedBox(height: 6),
            Text(
              value,
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              label,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 11, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildCategoryBreakdown(List<Report> reports) {
    if (reports.isEmpty) {
      return [const Text('No incident data available')];
    }

    final Map<String, int> counts = {};
    for (var r in reports) {
      counts[r.category] = (counts[r.category] ?? 0) + 1;
    }

    return counts.entries.map((entry) {
      final percentage = (entry.value / reports.length * 100).toStringAsFixed(
        1,
      );
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          children: [
            Expanded(
              flex: 3,
              child: Text(
                entry.key,
                style: const TextStyle(fontWeight: FontWeight.w500),
              ),
            ),
            Expanded(
              flex: 5,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: entry.value / reports.length,
                  minHeight: 10,
                  color: AppTheme.pupMaroon,
                  backgroundColor: Colors.grey.shade200,
                ),
              ),
            ),
            const SizedBox(width: 12),
            Text(
              '${entry.value} ($percentage%)',
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
            ),
          ],
        ),
      );
    }).toList();
  }
}
