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
  int _docCurrentPage = 1;
  int _docRowsPerPage = 10;
  bool _exportCurrentPageOnly = false;
  final List<int> _docPageSizeOptions = [5, 10, 15, 25];

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
      final fileName = await AnalyticsService.downloadCsv(
        reports: reports,
        rowsPerPage: _docRowsPerPage,
      );
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
      await AnalyticsService.printPdfReport(
        reports: reports,
        rowsPerPage: _docRowsPerPage,
      );
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

        final totalDocPages = total == 0
            ? 1
            : ((total - 1) ~/ _docRowsPerPage) + 1;
        final safeDocPage = _docCurrentPage.clamp(1, totalDocPages);
        final docStartIdx = total == 0
            ? 0
            : (safeDocPage - 1) * _docRowsPerPage;
        final docEndIdx = (docStartIdx + _docRowsPerPage).clamp(0, total);
        final currentPageReports = total == 0
            ? <Report>[]
            : reports.sublist(docStartIdx, docEndIdx);
        final exportTargetReports = _exportCurrentPageOnly
            ? currentPageReports
            : reports;

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
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
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
                                      'Download paginated data for administrative reviews, inspections, and official reports',
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
                                        : () => _handleCsvDownload(
                                            exportTargetReports,
                                          ),
                                    icon: const Icon(
                                      Icons.file_download,
                                      size: 18,
                                    ),
                                    label: const Text('Excel CSV'),
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: AppTheme.pupMaroon,
                                      foregroundColor: Colors.white,
                                    ),
                                  ),
                                  ElevatedButton.icon(
                                    onPressed: _isExporting
                                        ? null
                                        : () => _handlePrintPdf(
                                            exportTargetReports,
                                          ),
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
                          const Divider(height: 20),
                          // Document Pagination Settings Row for Print PDF & Excel CSV
                          Wrap(
                            spacing: 16,
                            runSpacing: 8,
                            crossAxisAlignment: WrapCrossAlignment.center,
                            children: [
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Icon(
                                    Icons.menu_book_outlined,
                                    size: 16,
                                    color: AppTheme.pupMaroon,
                                  ),
                                  const SizedBox(width: 6),
                                  const Text(
                                    'Document Rows per Page:',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.black87,
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 8,
                                      vertical: 2,
                                    ),
                                    decoration: BoxDecoration(
                                      color: Colors.white,
                                      borderRadius: BorderRadius.circular(6),
                                      border: Border.all(
                                        color: Colors.grey.shade400,
                                      ),
                                    ),
                                    child: DropdownButton<int>(
                                      value: _docRowsPerPage,
                                      isDense: true,
                                      underline: const SizedBox(),
                                      items: _docPageSizeOptions
                                          .map(
                                            (s) => DropdownMenuItem(
                                              value: s,
                                              child: Text(
                                                '$s rows / page',
                                                style: const TextStyle(
                                                  fontSize: 12,
                                                  fontWeight: FontWeight.bold,
                                                ),
                                              ),
                                            ),
                                          )
                                          .toList(),
                                      onChanged: (val) {
                                        if (val != null) {
                                          setState(() {
                                            _docRowsPerPage = val;
                                            _docCurrentPage = 1;
                                          });
                                        }
                                      },
                                    ),
                                  ),
                                ],
                              ),
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Text(
                                    'Export Scope:',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.black87,
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  ChoiceChip(
                                    label: Text(
                                      'All Pages ($totalDocPages)',
                                      style: TextStyle(
                                        fontSize: 11,
                                        fontWeight: FontWeight.bold,
                                        color: !_exportCurrentPageOnly
                                            ? Colors.white
                                            : Colors.black87,
                                      ),
                                    ),
                                    selected: !_exportCurrentPageOnly,
                                    selectedColor: AppTheme.pupMaroon,
                                    backgroundColor: Colors.white,
                                    visualDensity: VisualDensity.compact,
                                    onSelected: (_) => setState(
                                      () => _exportCurrentPageOnly = false,
                                    ),
                                  ),
                                  const SizedBox(width: 6),
                                  ChoiceChip(
                                    label: Text(
                                      'Page $safeDocPage Only',
                                      style: TextStyle(
                                        fontSize: 11,
                                        fontWeight: FontWeight.bold,
                                        color: _exportCurrentPageOnly
                                            ? Colors.white
                                            : Colors.black87,
                                      ),
                                    ),
                                    selected: _exportCurrentPageOnly,
                                    selectedColor: AppTheme.pupMaroon,
                                    backgroundColor: Colors.white,
                                    visualDensity: VisualDensity.compact,
                                    onSelected: (_) => setState(
                                      () => _exportCurrentPageOnly = true,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Paginated Printable / Exportable Document Preview Card
                  _buildPaginatedDocumentPreviewCard(
                    settings: settings,
                    pageReports: currentPageReports,
                    totalItems: total,
                    startIndex: docStartIdx,
                    endIndex: docEndIdx,
                    currentPage: safeDocPage,
                    totalPages: totalDocPages,
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

  Widget _buildPaginatedDocumentPreviewCard({
    required AccessibilitySettings settings,
    required List<Report> pageReports,
    required int totalItems,
    required int startIndex,
    required int endIndex,
    required int currentPage,
    required int totalPages,
  }) {
    int startPage = (currentPage - 2).clamp(1, totalPages);
    int endPage = (startPage + 4).clamp(1, totalPages);
    if (endPage - startPage < 4) {
      startPage = (endPage - 4).clamp(1, totalPages);
    }
    final pageNumbers = [for (int p = startPage; p <= endPage; p++) p];

    return Card(
      color: settings.cardColor,
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(settings.cardBorderRadius),
        side: BorderSide(color: settings.borderColor),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(
                      Icons.description_outlined,
                      color: settings.accentColor,
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'Printable PDF & Excel Document Preview',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: settings.primaryTextColor,
                      ),
                    ),
                  ],
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: settings.accentColor.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    'Page $currentPage of $totalPages',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: settings.accentColor,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (pageReports.isEmpty)
              Padding(
                padding: const EdgeInsets.all(24),
                child: Center(
                  child: Text(
                    'No incident records to preview.',
                    style: TextStyle(color: settings.secondaryTextColor),
                  ),
                ),
              )
            else
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: DataTable(
                  headingRowHeight: 38,
                  dataRowMinHeight: 36,
                  dataRowMaxHeight: 44,
                  columnSpacing: 18,
                  headingRowColor: WidgetStateProperty.all(
                    settings.contrastMode == 'Dark Contrast'
                        ? const Color(0xFF2A2A2A)
                        : Colors.grey.shade100,
                  ),
                  columns: const [
                    DataColumn(
                      label: Text(
                        '#',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                    DataColumn(
                      label: Text(
                        'Date',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                    DataColumn(
                      label: Text(
                        'Title',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                    DataColumn(
                      label: Text(
                        'Category',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                    DataColumn(
                      label: Text(
                        'Location',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                    DataColumn(
                      label: Text(
                        'Status',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                    DataColumn(
                      label: Text(
                        'Reporter',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                  rows: List.generate(pageReports.length, (i) {
                    final r = pageReports[i];
                    final rowNum = startIndex + i + 1;
                    return DataRow(
                      cells: [
                        DataCell(
                          Text(
                            '$rowNum',
                            style: const TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        DataCell(
                          Text(
                            r.dateSubmitted,
                            style: const TextStyle(fontSize: 12),
                          ),
                        ),
                        DataCell(
                          ConstrainedBox(
                            constraints: const BoxConstraints(maxWidth: 180),
                            child: Text(
                              r.title,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ),
                        DataCell(
                          Text(
                            r.category,
                            style: const TextStyle(fontSize: 12),
                          ),
                        ),
                        DataCell(
                          ConstrainedBox(
                            constraints: const BoxConstraints(maxWidth: 140),
                            child: Text(
                              r.location,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(fontSize: 12),
                            ),
                          ),
                        ),
                        DataCell(
                          Text(
                            r.status,
                            style: const TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        DataCell(
                          Text(
                            r.isAnonymous ? 'Anonymous' : r.reporter,
                            style: const TextStyle(fontSize: 12),
                          ),
                        ),
                      ],
                    );
                  }),
                ),
              ),
            const Divider(height: 20),
            Row(
              children: [
                Text(
                  totalItems == 0
                      ? 'Showing 0 of 0 records'
                      : 'Showing ${startIndex + 1}–$endIndex of $totalItems records ($_docRowsPerPage / page)',
                  style: TextStyle(
                    fontSize: 12,
                    color: settings.secondaryTextColor,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const Spacer(),
                OutlinedButton.icon(
                  onPressed: currentPage > 1
                      ? () => setState(() => _docCurrentPage = currentPage - 1)
                      : null,
                  icon: const Icon(Icons.chevron_left, size: 16),
                  label: const Text('Prev', style: TextStyle(fontSize: 12)),
                  style: OutlinedButton.styleFrom(
                    visualDensity: VisualDensity.compact,
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                  ),
                ),
                const SizedBox(width: 6),
                ...pageNumbers.map((p) {
                  final isSel = p == currentPage;
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 2),
                    child: InkWell(
                      onTap: () => setState(() => _docCurrentPage = p),
                      borderRadius: BorderRadius.circular(6),
                      child: Container(
                        constraints: const BoxConstraints(
                          minWidth: 30,
                          minHeight: 30,
                        ),
                        padding: const EdgeInsets.symmetric(horizontal: 8),
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: isSel
                              ? settings.accentColor
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(
                            color: isSel
                                ? settings.accentColor
                                : settings.borderColor,
                          ),
                        ),
                        child: Text(
                          '$p',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: isSel
                                ? FontWeight.bold
                                : FontWeight.normal,
                            color: isSel
                                ? Colors.white
                                : settings.primaryTextColor,
                          ),
                        ),
                      ),
                    ),
                  );
                }),
                const SizedBox(width: 6),
                OutlinedButton.icon(
                  onPressed: currentPage < totalPages
                      ? () => setState(() => _docCurrentPage = currentPage + 1)
                      : null,
                  icon: const Icon(Icons.chevron_right, size: 16),
                  label: const Text('Next', style: TextStyle(fontSize: 12)),
                  style: OutlinedButton.styleFrom(
                    visualDensity: VisualDensity.compact,
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
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
