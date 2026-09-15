package Activity

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AnalyticsPrintHelper {

    fun downloadPdfReport(
        context: Context,
        reports: List<Report>,
        surveys: List<SurveyData> = emptyList(),
        settings: AccessibilitySettings
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "PUP_Campus_Analytics_$timestamp.html"
            val htmlContent = buildHtmlContent(reports, surveys)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(htmlContent.toByteArray(Charsets.UTF_8))
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, fileName)
                targetFile.writeText(htmlContent, Charsets.UTF_8)
            }

            Toast.makeText(context, "📥 Downloaded: $fileName to Downloads folder", Toast.LENGTH_LONG).show()

            // Also open PrintManager so they can save directly as PDF or print to hardware printer
            printAnalytics(context, reports, surveys, settings)

        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun printAnalytics(
        context: Context,
        reports: List<Report>,
        surveys: List<SurveyData> = emptyList(),
        settings: AccessibilitySettings
    ) {
        val htmlContent = buildHtmlContent(reports, surveys)

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("PUP_Campus_Analytics_Report")
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager.print("PUP_Campus_Analytics_Report", printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun buildHtmlContent(
        reports: List<Report>,
        surveys: List<SurveyData>
    ): String {
        val total = reports.size
        val inReview = reports.count { it.status == "In Review" }
        val inProgress = reports.count { it.status == "In Progress" }
        val resolved = reports.count { it.status == "Resolved" }
        val resolutionRate = if (total > 0) (resolved.toDouble() / total * 100) else 0.0

        val totalSurveys = surveys.size
        val completelySatisfied = surveys.count { it.qualityOfWork.contains("completely", ignoreCase = true) || it.qualityOfWork.contains("yes", ignoreCase = true) }
        val partiallySatisfied = surveys.count { it.qualityOfWork.contains("partially", ignoreCase = true) }
        val dissatisfied = surveys.count { it.qualityOfWork.contains("no", ignoreCase = true) || it.qualityOfWork.contains("needs work", ignoreCase = true) }

        val categoryCounts = reports.groupBy { it.category }.mapValues { it.value.size }
        val locationCounts = reports.filter { it.status != "Resolved" }
            .groupBy { it.location }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }

        val currentDate = SimpleDateFormat("MMMM d, yyyy - hh:mm a", Locale.getDefault()).format(Date())

        val aiSummary = if (total == 0) {
            "No campus reports filed yet. All facilities are operational."
        } else {
            val topCategory = categoryCounts.maxByOrNull { it.value }?.key ?: "General"
            val topLocation = locationCounts.firstOrNull()?.key ?: "Campus-wide"
            "Campus facility operations summary: Out of $total total filed reports, $resolved have been resolved (${String.format(Locale.getDefault(), "%.1f", resolutionRate)}%). The most active issue category is '$topCategory'. Location '$topLocation' currently requires highest administrative focus with ${locationCounts.firstOrNull()?.value ?: 0} active reports."
        }

        return StringBuilder().apply {
            append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
            append("<style>")
            append("body { font-family: Arial, sans-serif; margin: 20px; color: #222; }")
            append(".header { text-align: center; border-bottom: 2px solid #E1001B; padding-bottom: 10px; margin-bottom: 20px; }")
            append(".header h1 { margin: 0; color: #E1001B; font-size: 22px; }")
            append(".header h2 { margin: 5px 0 0 0; color: #555; font-size: 14px; font-weight: normal; }")
            append(".meta { font-size: 12px; color: #666; margin-bottom: 20px; }")
            append(".summary-box { background-color: #F8F9FA; border-left: 4px solid #E1001B; padding: 12px 16px; margin-bottom: 20px; border-radius: 4px; }")
            append(".summary-box h3 { margin: 0 0 6px 0; font-size: 14px; color: #E1001B; }")
            append(".summary-box p { margin: 0; font-size: 13px; line-height: 1.4; color: #333; }")
            append(".stats-grid { display: flex; justify-content: space-between; margin-bottom: 20px; }")
            append(".stat-card { background: #FFF; border: 1px solid #E0E0E0; border-radius: 6px; padding: 10px; text-align: center; width: 22%; }")
            append(".stat-number { font-size: 20px; font-weight: bold; color: #E1001B; }")
            append(".stat-label { font-size: 11px; color: #666; }")
            append("table { width: 100%; border-collapse: collapse; margin-top: 15px; font-size: 12px; }")
            append("th, td { border: 1px solid #DDD; padding: 8px; text-align: left; }")
            append("th { background-color: #F2F2F2; font-weight: bold; color: #333; }")
            append(".status-resolved { color: #2E7D32; font-weight: bold; }")
            append(".status-progress { color: #1976D2; font-weight: bold; }")
            append(".status-review { color: #E1001B; font-weight: bold; }")
            append(".footer { margin-top: 30px; border-top: 1px solid #DDD; padding-top: 15px; font-size: 11px; color: #777; display: flex; justify-content: space-between; }")
            append("</style></head><body>")

            append("<div class='header'>")
            append("<h1>POLYTECHNIC UNIVERSITY OF THE PHILIPPINES</h1>")
            append("<h2>Campus Safety & Facilities Management - Executive Report</h2>")
            append("</div>")

            append("<div class='meta'> Generated on: ").append(currentDate).append(" | Scope: All Campus Reports</div>")

            append("<div class='summary-box'>")
            append("<h3>📊 Executive Insights</h3>")
            append("<p>").append(aiSummary).append("</p>")
            append("</div>")

            if (totalSurveys > 0) {
                append("<div class='summary-box' style='border-left-color:#1976D2;'>")
                append("<h3>📋 Student Resolution Survey Feedback ($totalSurveys Answered)</h3>")
                append("<p>• 🟢 Completely Satisfied: $completelySatisfied</p>")
                append("<p>• 🟡 Partially Satisfied: $partiallySatisfied</p>")
                append("<p>• 🔴 Not Satisfied / Needs Work: $dissatisfied</p>")
                append("</div>")
            }

            append("<div class='stats-grid'>")
            append("<div class='stat-card'><div class='stat-number'>").append(total).append("</div><div class='stat-label'>Total Reports</div></div>")
            append("<div class='stat-card'><div class='stat-number' style='color:#E1001B;'>").append(inReview).append("</div><div class='stat-label'>In Review</div></div>")
            append("<div class='stat-card'><div class='stat-number' style='color:#1976D2;'>").append(inProgress).append("</div><div class='stat-label'>In Progress</div></div>")
            append("<div class='stat-card'><div class='stat-number' style='color:#2E7D32;'>").append(resolved).append("</div><div class='stat-label'>Resolved</div></div>")
            append("</div>")

            append("<h3>Campus Incident Registry</h3>")
            append("<table>")
            append("<thead><tr><th>Title</th><th>Category</th><th>Location</th><th>Status</th><th>Date</th><th>Reporter</th></tr></thead><tbody>")

            reports.forEach { report ->
                val statusClass = when (report.status) {
                    "Resolved" -> "status-resolved"
                    "In Progress" -> "status-progress"
                    else -> "status-review"
                }
                val reporterName = if (report.isAnonymous || report.reporter == "Anonymous Student") "Anonymous Student" else report.reporter

                append("<tr>")
                append("<td>").append(escapeHtml(report.title)).append("</td>")
                append("<td>").append(escapeHtml(report.category)).append("</td>")
                append("<td>").append(escapeHtml(report.location)).append("</td>")
                append("<td class='").append(statusClass).append("'>").append(escapeHtml(report.status)).append("</td>")
                append("<td>").append(escapeHtml(report.dateSubmitted)).append("</td>")
                append("<td>").append(escapeHtml(reporterName)).append("</td>")
                append("</tr>")
            }

            append("</tbody></table>")

            append("<div class='footer'>")
            append("<div>Report Monitoring System &bull; Confidential</div>")
            append("<div>Administrative Signature: ______________________</div>")
            append("</div>")

            append("</body></html>")
        }.toString()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}