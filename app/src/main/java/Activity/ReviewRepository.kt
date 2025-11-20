package Activity


// -- Use this class in ReviewRepository as well for consistency
data class Review(
    val rating: Int,
    val text: String,
    var likes: Int = 0,
    var dislikes: Int = 0,
    var adminLiked: Boolean = false
)

object ReviewRepository {
    val reviewsByReport = mutableMapOf<String, MutableList<Review>>()
}
