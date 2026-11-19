import 'dart:convert';
import 'dart:typed_data';

import 'package:file_saver/file_saver.dart';
import 'package:intl/intl.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';

import '../models/report_model.dart';
import '../models/survey_model.dart';

class AnalyticsService {
  // Generates CSV with pagination and triggers cross-platform download
  static Future<String> downloadCsv({
    required List<Report> reports,
    List<PerformanceSurvey> surveys = const [],
    int rowsPerPage = 10,
  }) async {
    final total = reports.length;
    final inReview = reports.where((r) => r.status == 'In Review').length;
    final inProgress = reports.where((r) => r.status == 'In Progress').length;
    final resolved = reports.where((r) => r.status == 'Resolved').length;
    final totalPages = total == 0 ? 1 : ((total - 1) ~/ rowsPerPage) + 1;

    final totalSurveys = surveys.length;
    final highRating = surveys.where((s) => s.overallRating >= 4).length;
    final fastSpeed = surveys
        .where((s) => s.resolutionSpeed.toLowerCase().contains('fast'))
        .length;
    final excellentStaff = surveys
        .where(
          (s) => s.staffProfessionalism.toLowerCase().contains('excellent'),
        )
        .length;
    final safeImprovement = surveys
        .where(
          (s) => s.safetyImprovement.toLowerCase().contains('significantly'),
        )
        .length;

    final timestamp = DateFormat('yyyyMMdd_HHmmss').format(DateTime.now());
    final nowFormatted = DateFormat('yyyy-MM-dd HH:mm:ss')
        .format(DateTime.now());

    final buffer = StringBuffer();
    // UTF-8 BOM for Microsoft Excel
    buffer.write('\uFEFF');
    buffer.writeln('PUP-ALERT CAMPUS SAFETY & FACILITIES ANALYTICS REPORT');
    buffer.writeln('Generated Date,$nowFormatted');
    buffer.writeln(
      'Total Pages,$totalPages,Rows Per Page,$rowsPerPage,Total Reports,$total',
    );
    buffer.writeln(
      'Total Reports,$total,In Review,$inReview,In Progress,$inProgress,Resolved,$resolved',
    );
    buffer.writeln(
      'Total Surveys Answered,$totalSurveys,High Rating (4-5 Stars),$highRating,Fast Resolution Speed,$fastSpeed,Excellent Staff Professionalism,$excellentStaff,Significantly Safer Impact,$safeImprovement',
    );
    buffer.writeln('');

    if (reports.isEmpty) {
      buffer.writeln('--- PAGE 1 OF 1 (0 Records) ---');
      buffer.writeln(
        'No.,Page,Report ID,Title,Category,Location,Status,Date Submitted,Reporter,Is Anonymous,Average Rating,Rating Count',
      );
    } else {
      for (int page = 1; page <= totalPages; page++) {
        final startIdx = (page - 1) * rowsPerPage;
        final endIdx = (startIdx + rowsPerPage).clamp(0, total);
        buffer.writeln(
          '--- PAGE $page OF $totalPages (Showing Records ${startIdx + 1} to $endIdx of $total) ---',
        );
        buffer.writeln(
          'No.,Page,Report ID,Title,Category,Location,Status,Date Submitted,Reporter,Is Anonymous,Average Rating,Rating Count',
        );

        for (int i = startIdx; i < endIdx; i++) {
          final report = reports[i];
          final reporterName = report.isAnonymous
              ? 'Anonymous Student'
              : report.reporter;
          final row = [
            '${i + 1}',
            _escapeCsv('Page $page of $totalPages'),
            _escapeCsv(report.id),
            _escapeCsv(report.title),
            _escapeCsv(report.category),
            _escapeCsv(report.location),
            _escapeCsv(report.status),
            _escapeCsv(report.dateSubmitted),
            _escapeCsv(reporterName),
            report.isAnonymous ? 'Yes' : 'No',
            report.averageRating.toString(),
            report.ratingCount.toString(),
          ];
          buffer.writeln(row.join(','));
        }
        buffer.writeln('End of Page $page of $totalPages');
        buffer.writeln('');
      }
    }

    final bytes = Uint8List.fromList(utf8.encode(buffer.toString()));
    final fileName = 'PUP_Campus_Analytics_$timestamp.csv';

    await FileSaver.instance.saveFile(
      name: fileName,
      bytes: bytes,
      mimeType: MimeType.csv,
    );

    return fileName;
  }

  // Generates paginated PDF with page numbers and opens system print dialog
  static Future<void> printPdfReport({
    required List<Report> reports,
    List<PerformanceSurvey> surveys = const [],
    int rowsPerPage = 10,
  }) async {
    final pdf = pw.Document();

    final total = reports.length;
    final inReview = reports.where((r) => r.status == 'In Review').length;
    final inProgress = reports.where((r) => r.status == 'In Progress').length;
    final resolved = reports.where((r) => r.status == 'Resolved').length;
    final nowFormatted = DateFormat('yyyy-MM-dd HH:mm').format(DateTime.now());
    final totalChunks = total == 0 ? 1 : ((total - 1) ~/ rowsPerPage) + 1;

    for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
      final startIdx = total == 0 ? 0 : chunkIndex * rowsPerPage;
      final endIdx = (startIdx + rowsPerPage).clamp(0, total);
      final pageReports = total == 0
          ? <Report>[]
          : reports.sublist(startIdx, endIdx);
      final pageNum = chunkIndex + 1;

      pdf.addPage(
        pw.MultiPage(
          pageFormat: PdfPageFormat.a4,
          margin: const pw.EdgeInsets.all(32),
          header: (pw.Context context) {
            return pw.Column(
              crossAxisAlignment: pw.CrossAxisAlignment.start,
              children: [
                pw.Row(
                  mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                  children: [
                    pw.Column(
                      crossAxisAlignment: pw.CrossAxisAlignment.start,
                      children: [
                        pw.Text(
                          'POLYTECHNIC UNIVERSITY OF THE PHILIPPINES',
                          style: pw.TextStyle(
                            fontSize: 13,
                            fontWeight: pw.FontWeight.bold,
                            color: const PdfColor.fromInt(0xFF800000),
                          ),
                        ),
                        pw.Text(
                          'Campus Safety & Incident Management Report (PUP-ALERT)',
                          style: const pw.TextStyle(
                            fontSize: 9,
                            color: PdfColors.grey700,
                          ),
                        ),
                      ],
                    ),
                    pw.Column(
                      crossAxisAlignment: pw.CrossAxisAlignment.end,
                      children: [
                        pw.Text(
                          'Generated: $nowFormatted',
                          style: const pw.TextStyle(
                            fontSize: 9,
                            color: PdfColors.grey600,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
                pw.SizedBox(height: 6),
                pw.Divider(color: PdfColors.grey400, thickness: 0.8),
                pw.SizedBox(height: 8),
              ],
            );
          },
          footer: (pw.Context context) {
            return pw.Container(
              alignment: pw.Alignment.centerRight,
              margin: const pw.EdgeInsets.only(top: 10),
              padding: const pw.EdgeInsets.only(top: 6),
              decoration: const pw.BoxDecoration(
                border: pw.Border(
                  top: pw.BorderSide(color: PdfColors.grey400, width: 0.5),
                ),
              ),
              child: pw.Row(
                mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                children: [
                  pw.Text(
                    'PUP-ALERT Official Incident Analytics Document',
                    style: const pw.TextStyle(
                      fontSize: 8,
                      color: PdfColors.grey600,
                    ),
                  ),
                  pw.Text(
                    'Page ${context.pageNumber} of ${context.pagesCount}',
                    style: pw.TextStyle(
                      fontSize: 10,
                      fontWeight: pw.FontWeight.bold,
                      color: const PdfColor.fromInt(0xFF800000),
                    ),
                  ),
                ],
              ),
            );
          },
          build: (pw.Context context) {
            return [
              if (chunkIndex == 0) ...[
                pw.Container(
                  padding: const pw.EdgeInsets.all(10),
                  decoration: pw.BoxDecoration(
                    border: pw.Border.all(color: PdfColors.grey400),
                    borderRadius: const pw.BorderRadius.all(
                      pw.Radius.circular(6),
                    ),
                    color: PdfColors.grey100,
                  ),
                  child: pw.Row(
                    mainAxisAlignment: pw.MainAxisAlignment.spaceAround,
                    children: [
                      _buildStatItem('TOTAL REPORTS', total.toString()),
                      _buildStatItem('IN REVIEW', inReview.toString()),
                      _buildStatItem('IN PROGRESS', inProgress.toString()),
                      _buildStatItem('RESOLVED', resolved.toString()),
                    ],
                  ),
                ),
                pw.SizedBox(height: 14),
              ],
              pw.Row(
                mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                children: [
                  pw.Text(
                    'Incident Reports List (Page $pageNum of $totalChunks)',
                    style: pw.TextStyle(
                      fontSize: 11,
                      fontWeight: pw.FontWeight.bold,
                    ),
                  ),
                  pw.Text(
                    total == 0
                        ? 'Showing 0 of 0 records'
                        : 'Showing ${startIdx + 1}–$endIdx of $total records ($rowsPerPage / page)',
                    style: const pw.TextStyle(
                      fontSize: 9,
                      color: PdfColors.grey700,
                    ),
                  ),
                ],
              ),
              pw.SizedBox(height: 8),
              pw.TableHelper.fromTextArray(
                headers: [
                  '#',
                  'Date',
                  'Title',
                  'Category',
                  'Location',
                  'Status',
                  'Reporter',
                ],
                data: List.generate(pageReports.length, (idx) {
                  final r = pageReports[idx];
                  final rowNum = startIdx + idx + 1;
                  return [
                    '$rowNum',
                    r.dateSubmitted,
                    r.title,
                    r.category,
                    r.location,
                    r.status,
                    r.isAnonymous ? 'Anonymous' : r.reporter,
                  ];
                }),
                border: pw.TableBorder.all(
                  color: PdfColors.grey300,
                  width: 0.5,
                ),
                headerStyle: pw.TextStyle(
                  fontSize: 9,
                  fontWeight: pw.FontWeight.bold,
                ),
                cellStyle: const pw.TextStyle(fontSize: 8),
                headerDecoration: const pw.BoxDecoration(
                  color: PdfColors.grey200,
                ),
                cellPadding: const pw.EdgeInsets.symmetric(
                  horizontal: 4,
                  vertical: 4,
                ),
              ),
            ];
          },
        ),
      );
    }

    await Printing.layoutPdf(
      onLayout: (PdfPageFormat format) async => pdf.save(),
      name:
          'PUP_Campus_Analytics_${DateFormat('yyyyMMdd').format(DateTime.now())}.pdf',
    );
  }

  static pw.Widget _buildStatItem(String label, String value) {
    return pw.Column(
      children: [
        pw.Text(
          label,
          style: const pw.TextStyle(fontSize: 8, color: PdfColors.grey700),
        ),
        pw.SizedBox(height: 2),
        pw.Text(
          value,
          style: pw.TextStyle(fontSize: 14, fontWeight: pw.FontWeight.bold),
        ),
      ],
    );
  }

  static String _escapeCsv(String text) {
    if (text.contains(',') || text.contains('"') || text.contains('\n')) {
      return '"${text.replaceAll('"', '""')}"';
    }
    return text;
  }
}
