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
  // Generates CSV and triggers cross-platform download
  static Future<String> downloadCsv({
    required List<Report> reports,
    List<PerformanceSurvey> surveys = const [],
  }) async {
    final total = reports.length;
    final inReview = reports.where((r) => r.status == 'In Review').length;
    final inProgress = reports.where((r) => r.status == 'In Progress').length;
    final resolved = reports.where((r) => r.status == 'Resolved').length;

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
    buffer.writeln('PUP CAMPUS SAFETY & FACILITIES ANALYTICS REPORT');
    buffer.writeln('Generated Date,$nowFormatted');
    buffer.writeln(
      'Total Reports,$total,In Review,$inReview,In Progress,$inProgress,Resolved,$resolved',
    );
    buffer.writeln(
      'Total Surveys Answered,$totalSurveys,High Rating (4-5 Stars),$highRating,Fast Resolution Speed,$fastSpeed,Excellent Staff Professionalism,$excellentStaff,Significantly Safer Impact,$safeImprovement',
    );
    buffer.writeln('');
    buffer.writeln(
      'Report ID,Title,Category,Location,Status,Date Submitted,Reporter,Is Anonymous,Average Rating,Rating Count',
    );

    for (final report in reports) {
      final reporterName = report.isAnonymous
          ? 'Anonymous Student'
          : report.reporter;
      final row = [
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

    final bytes = Uint8List.fromList(utf8.encode(buffer.toString()));
    final fileName = 'PUP_Campus_Analytics_$timestamp.csv';

    await FileSaver.instance.saveFile(
      name: fileName,
      bytes: bytes,
      mimeType: MimeType.csv,
    );

    return fileName;
  }

  // Generates PDF and opens system print dialog
  static Future<void> printPdfReport({
    required List<Report> reports,
    List<PerformanceSurvey> surveys = const [],
  }) async {
    final pdf = pw.Document();

    final total = reports.length;
    final inReview = reports.where((r) => r.status == 'In Review').length;
    final inProgress = reports.where((r) => r.status == 'In Progress').length;
    final resolved = reports.where((r) => r.status == 'Resolved').length;
    final nowFormatted = DateFormat('yyyy-MM-dd HH:mm').format(DateTime.now());

    pdf.addPage(
      pw.MultiPage(
        pageFormat: PdfPageFormat.a4,
        margin: const pw.EdgeInsets.all(32),
        build: (pw.Context context) {
          return [
            pw.Header(
              level: 0,
              child: pw.Row(
                mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                children: [
                  pw.Column(
                    crossAxisAlignment: pw.CrossAxisAlignment.start,
                    children: [
                      pw.Text(
                        'POLYTECHNIC UNIVERSITY OF THE PHILIPPINES',
                        style: pw.TextStyle(
                          fontSize: 14,
                          fontWeight: pw.FontWeight.bold,
                          color: const PdfColor.fromInt(0xFF800000),
                        ),
                      ),
                      pw.Text(
                        'Campus Safety & Incident Management Report (PUP ALERT)',
                        style: const pw.TextStyle(
                          fontSize: 10,
                          color: PdfColors.grey700,
                        ),
                      ),
                    ],
                  ),
                  pw.Text(
                    nowFormatted,
                    style: const pw.TextStyle(
                      fontSize: 10,
                      color: PdfColors.grey600,
                    ),
                  ),
                ],
              ),
            ),
            pw.SizedBox(height: 12),
            pw.Container(
              padding: const pw.EdgeInsets.all(10),
              decoration: pw.BoxDecoration(
                border: pw.Border.all(color: PdfColors.grey400),
                borderRadius: const pw.BorderRadius.all(pw.Radius.circular(6)),
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
            pw.SizedBox(height: 16),
            pw.Text(
              'Incident Reports List',
              style: pw.TextStyle(fontSize: 12, fontWeight: pw.FontWeight.bold),
            ),
            pw.SizedBox(height: 8),
            pw.TableHelper.fromTextArray(
              headers: [
                'Date',
                'Title',
                'Category',
                'Location',
                'Status',
                'Reporter',
              ],
              data: reports.map((r) {
                return [
                  r.dateSubmitted.split(' ').first,
                  r.title,
                  r.category,
                  r.location,
                  r.status,
                  r.isAnonymous ? 'Anonymous' : r.reporter,
                ];
              }).toList(),
              border: pw.TableBorder.all(color: PdfColors.grey300, width: 0.5),
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
                vertical: 3,
              ),
            ),
          ];
        },
      ),
    );

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
