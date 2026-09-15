package Activity

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AnalyticsExcelHelper {

    fun downloadExcel(
        context: Context,
        reports: List<Report>,
        surveys: List<SurveyData> = emptyList()
    ) {
        try {
            val total = reports.size
            val inReview = reports.count { it.status == "In Review" }
            val inProgress = reports.count { it.status == "In Progress" }
            val resolved = reports.count { it.status == "Resolved" }

            val totalSurveys = surveys.size
            val completelySatisfied = surveys.count { it.qualityOfWork.contains("completely", ignoreCase = true) || it.qualityOfWork.contains("yes", ignoreCase = true) }
            val partiallySatisfied = surveys.count { it.qualityOfWork.contains("partially", ignoreCase = true) }
            val dissatisfied = surveys.count { it.qualityOfWork.contains("no", ignoreCase = true) || it.qualityOfWork.contains("needs work", ignoreCase = true) }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "PUP_Campus_Analytics_$timestamp.csv"

            val csvContent = StringBuilder().apply {
                // UTF-8 BOM for Microsoft Excel auto-encoding
                append("\uFEFF")
                append("PUP CAMPUS SAFETY & FACILITIES ANALYTICS REPORT\n")
                append("Generated Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                append("Total Reports,$total,In Review,$inReview,In Progress,$inProgress,Resolved,$resolved\n")
                append("Total Surveys Answered,$totalSurveys,Completely Satisfied,$completelySatisfied,Partially Satisfied,$partiallySatisfied,Not Satisfied / Needs Work,$dissatisfied\n\n")

                append("Report ID,Title,Category,Location,Status,Date Submitted,Reporter,Is Anonymous,Average Rating,Rating Count\n")

                reports.forEach { report ->
                    val reporterName = if (report.isAnonymous || report.reporter == "Anonymous Student") {
                        "Anonymous Student"
                    } else {
                        report.reporter
                    }

                    val row = listOf(
                        escapeCsv(report.id),
                        escapeCsv(report.title),
                        escapeCsv(report.category),
                        escapeCsv(report.location),
                        escapeCsv(report.status),
                        escapeCsv(report.dateSubmitted),
                        escapeCsv(reporterName),
                        if (report.isAnonymous) "TRUE" else "FALSE",
                        report.averageRating.toString(),
                        report.ratingCount.toString()
                    ).joinToString(",")

                    append(row).append("\n")
                }
            }.toString()

            // 1. Download/Save directly to public Downloads folder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, fileName)
                targetFile.writeText(csvContent, Charsets.UTF_8)
            }

            // 2. Also save to cache and launch Share/Open Intent
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(csvContent, Charsets.UTF_8)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "PUP Campus Safety & Facilities Analytics Report")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share or Open CSV Report"))

            Toast.makeText(context, "📥 Downloaded: $fileName to Downloads folder", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to download Excel report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeCsv(text: String): String {
        val escaped = text.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}