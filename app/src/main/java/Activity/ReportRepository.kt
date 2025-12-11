package Activity

import androidx.compose.runtime.mutableStateListOf

data class Report(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val location: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    var status: String = "Pending",
    val dateSubmitted: String = "",
    val reporter: String = "",
    val timestamp: Long = 0,
    val resolvedTimestamp: Long = 0
)

object ReportRepository {
    val reports = mutableStateListOf<Report>()
}
