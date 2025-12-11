package Activity

import androidx.compose.runtime.mutableStateListOf

// -- Use this class in ReviewRepository as well for consistency
data class Review(
    val id: String = "",
    val rating: Int,
    val text: String,
    var likes: Int = 0,
    var dislikes: Int = 0,
    var adminLiked: Boolean = false
)

object ReviewRepository {
    // Ideally this would be fetched from Firestore per report ID
    val reviewsByReport = mutableMapOf<String, MutableList<Review>>()
}
