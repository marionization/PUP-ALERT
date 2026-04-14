package Activity

import androidx.compose.runtime.mutableStateListOf

data class Report(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val location: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val mediaUrl: String = "",
    val mediaType: String = "",
    val status: String = "Pending",
    val dateSubmitted: String = "",
    val reporter: String = "",
    val timestamp: Long = 0L,
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0
)

object ReportRepository {
    val reports = mutableStateListOf<Report>()
}