package Activity

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf

data class Report(
    val title: String,
    val category: String,
    val location: String,
    val description: String,
    val imageUri: Uri?,
    var status: String,
    val dateSubmitted: String,
    val reporter: String
)

object ReportRepository {
    val reports = mutableStateListOf<Report>()
}

