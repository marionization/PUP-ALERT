package Activity

data class Review(
    val id: String = "",
    val rating: Int = 5,
    val text: String = "",
    val reviewerName: String = "",
    val reviewerStudentId: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0,
    val adminLiked: Boolean = false
)

object ReviewRepository {
    val reviewsByReport = mutableMapOf<String, MutableList<Review>>()
}