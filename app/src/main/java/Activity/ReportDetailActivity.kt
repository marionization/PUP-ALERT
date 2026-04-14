package Activity

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import Activity.ui.theme.SeriousModeTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class ReportDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val role = intent.getStringExtra("role") ?: "Student"
        val title = intent.getStringExtra("title") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val location = intent.getStringExtra("location") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val imageUriStr = intent.getStringExtra("imageUri")
        val mediaUrl = intent.getStringExtra("mediaUrl") ?: ""
        val mediaType = intent.getStringExtra("mediaType") ?: ""
        val status = intent.getStringExtra("status") ?: ""
        val dateSubmitted = intent.getStringExtra("dateSubmitted") ?: ""
        val reporter = intent.getStringExtra("reporter") ?: ""
        val reportId = intent.getStringExtra("id") ?: ""

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val fullUserName = prefs.getString("user_name", "Student") ?: "Student"

        Log.d("ReportDetailActivity", "Received reportId: $reportId")

        val imageUri = if (!imageUriStr.isNullOrEmpty()) Uri.parse(imageUriStr) else null

        setContent {
            SeriousModeTheme {
                ReportDetailScreen(
                    onBackClick = { finish() },
                    title = title,
                    category = category,
                    location = location,
                    description = description,
                    imageUri = imageUri,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    status = status,
                    dateSubmitted = dateSubmitted,
                    reporter = reporter,
                    isAdmin = role == "Administrator",
                    currentUserName = fullUserName,
                    reportId = reportId
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    onBackClick: () -> Unit,
    title: String,
    category: String,
    location: String,
    description: String,
    imageUri: Uri?,
    mediaUrl: String,
    mediaType: String,
    status: String,
    dateSubmitted: String,
    reporter: String,
    isAdmin: Boolean = false,
    currentUserName: String,
    reportId: String
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val studentId = prefs.getString("student_id", "") ?: ""

    var selectedStatus by remember { mutableStateOf(status) }
    val statusOptions = listOf("Pending", "In Progress", "Resolved")
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var userReviewText by remember { mutableStateOf("") }
    var userRating by remember { mutableIntStateOf(5) }
    var hasExistingUserReview by remember { mutableStateOf(false) }

    var reportAverageRating by remember { mutableDoubleStateOf(0.0) }
    var reportRatingCount by remember { mutableIntStateOf(0) }

    val reviewerKey = remember(studentId, currentUserName) {
        if (studentId.isNotBlank()) {
            studentId.trim().lowercase()
        } else {
            currentUserName.trim().lowercase()
        }
    }

    LaunchedEffect(reportId) {
        if (reportId.isNotEmpty()) {
            db.collection("reports").document(reportId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        reportAverageRating = snapshot.getDouble("averageRating") ?: 0.0
                        reportRatingCount = (snapshot.getLong("ratingCount") ?: 0L).toInt()
                        selectedStatus = snapshot.getString("status") ?: selectedStatus
                    }
                }

            db.collection("reports").document(reportId).collection("reviews")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val loadedReviews = snapshot.documents.map { doc ->
                            Review(
                                id = doc.id,
                                rating = (doc.getLong("rating") ?: 5).toInt(),
                                text = doc.getString("text") ?: "",
                                reviewerName = doc.getString("reviewerName") ?: "",
                                reviewerStudentId = doc.getString("reviewerStudentId") ?: "",
                                likes = (doc.getLong("likes") ?: 0).toInt(),
                                dislikes = (doc.getLong("dislikes") ?: 0).toInt(),
                                adminLiked = doc.getBoolean("adminLiked") ?: false
                            )
                        }
                        reviews = loadedReviews
                    }
                }

            db.collection("reports")
                .document(reportId)
                .collection("reviews")
                .document(reviewerKey)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        hasExistingUserReview = true
                        userRating = (snapshot.getLong("rating") ?: 5L).toInt()
                        userReviewText = snapshot.getString("text") ?: ""
                    } else {
                        hasExistingUserReview = false
                        userRating = 5
                        userReviewText = ""
                    }
                }
        }
    }

    val statusColor = when (selectedStatus) {
        "Resolved" -> Color(0xFF17B169)
        "In Progress" -> Color(0xFFFFC107)
        else -> Color(0xFFE1001B)
    }

    val canReview = selectedStatus == "Resolved"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.White)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBackClick() }
                .padding(top = 10.dp, start = 8.dp, bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Go Back",
                tint = Color.Black
            )
            Spacer(Modifier.width(4.dp))
            Text("Back to Reports", fontSize = 15.sp, color = Color.Black)
        }

        when {
            mediaType == "image" && mediaUrl.isNotBlank() -> {
                Image(
                    painter = rememberAsyncImagePainter(mediaUrl),
                    contentDescription = "Report Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }

            mediaType == "video" && mediaUrl.isNotBlank() -> {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(mediaUrl))
                            setOnPreparedListener { mp ->
                                mp.isLooping = false
                                seekTo(1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color.Black)
                )
            }

            imageUri != null -> {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Report Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No media attached", color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Normal)
                    Text(
                        selectedStatus,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(statusColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    category,
                    color = Color(0xFFE1001B),
                    modifier = Modifier
                        .background(Color(0xFFFDECEC), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(location, fontSize = 12.sp, color = Color.Gray)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(dateSubmitted, fontSize = 12.sp, color = Color.Gray)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(reporter, fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(Modifier.height(16.dp))

                Text("Description", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(description)

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Average Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (reportRatingCount > 0) {
                            "${String.format("%.1f", reportAverageRating)} / 5"
                        } else {
                            "No ratings yet"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (reportRatingCount > 0) Color(0xFF444444) else Color.Gray
                    )

                    if (reportRatingCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "($reportRatingCount review${if (reportRatingCount > 1) "s" else ""})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        if (isAdmin && reportId.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEC))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFFE1001B))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Admin Controls",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFE1001B)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("Update Report Status", fontWeight = FontWeight.Medium, fontSize = 14.sp)

                    ExposedDropdownMenuBox(
                        expanded = statusDropdownExpanded,
                        onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStatus,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = statusDropdownExpanded,
                            onDismissRequest = { statusDropdownExpanded = false }
                        ) {
                            statusOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        if (selectedStatus != option) {
                                            val oldStatus = selectedStatus
                                            selectedStatus = option
                                            statusDropdownExpanded = false

                                            val updates = hashMapOf<String, Any>(
                                                "status" to option,
                                                "resolvedTimestamp" to if (option == "Resolved") System.currentTimeMillis() else 0L
                                            )

                                            db.collection("reports").document(reportId)
                                                .update(updates)
                                                .addOnSuccessListener {
                                                    if (option == "In Progress" || option == "Resolved") {
                                                        val studentTitle = if (option == "Resolved") {
                                                            "Report Resolved"
                                                        } else {
                                                            "Report Update"
                                                        }

                                                        val studentMessage = if (option == "Resolved") {
                                                            "Your report \"$title\" has been resolved. Please check the latest update."
                                                        } else {
                                                            "Your report \"$title\" is now being reviewed by the staff."
                                                        }

                                                        val adminTitle = "Report Status Changed"
                                                        val adminMessage = "A report \"$title\" is now marked as $option."

                                                        val studentNotification = hashMapOf(
                                                            "reportId" to reportId,
                                                            "title" to studentTitle,
                                                            "message" to studentMessage,
                                                            "reportTitle" to title,
                                                            "status" to option,
                                                            "reporter" to reporter,
                                                            "targetRole" to "Student",
                                                            "targetUser" to reporter,
                                                            "type" to "report_status",
                                                            "read" to false,
                                                            "timestamp" to Timestamp.now()
                                                        )

                                                        val adminNotification = hashMapOf(
                                                            "reportId" to reportId,
                                                            "title" to adminTitle,
                                                            "message" to adminMessage,
                                                            "reportTitle" to title,
                                                            "status" to option,
                                                            "reporter" to reporter,
                                                            "targetRole" to "Admin",
                                                            "targetUser" to "",
                                                            "type" to "report_status",
                                                            "read" to false,
                                                            "timestamp" to Timestamp.now()
                                                        )

                                                        db.collection("notifications").add(studentNotification)
                                                        db.collection("notifications").add(adminNotification)

                                                        NotificationHelper.createNotificationChannel(context)
                                                        NotificationHelper.showNotification(
                                                            context = context,
                                                            title = studentTitle,
                                                            message = studentMessage
                                                        )
                                                    }

                                                    Toast.makeText(
                                                        context,
                                                        "Status updated to $option",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    selectedStatus = oldStatus
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to update status: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        } else {
                                            statusDropdownExpanded = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        "Change the status to keep everyone informed about the progress",
                        color = Color(0xFFE1001B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (selectedStatus == "Resolved") {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                db.collection("reports").document(reportId).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Report Deleted", Toast.LENGTH_SHORT).show()
                                        onBackClick()
                                    }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Resolved Report")
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Reviews",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isAdmin || canReview) {
                if (reviews.isEmpty()) {
                    Text(
                        "No reviews yet.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    reviews.forEach { review ->
                        ReviewItem(
                            review = review,
                            isAdmin = isAdmin,
                            onLikeClick = {
                                if (isAdmin && reportId.isNotEmpty() && review.id.isNotEmpty()) {
                                    db.collection("reports").document(reportId)
                                        .collection("reviews")
                                        .document(review.id)
                                        .update(
                                            mapOf(
                                                "adminLiked" to !review.adminLiked,
                                                "likes" to if (!review.adminLiked) review.likes + 1 else maxOf(review.likes - 1, 0)
                                            )
                                        )
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Reviews Locked",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE1001B)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Reviews will be available once this report is marked as Resolved.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            if (!isAdmin && reportId.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                if (canReview) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = if (hasExistingUserReview) "Update Your Review" else "Leave a Review",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { i ->
                                    Icon(
                                        imageVector = if (i <= userRating) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = "Star $i",
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable { userRating = i }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = userReviewText,
                                onValueChange = { userReviewText = it },
                                placeholder = { Text("Write your feedback...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                maxLines = 4
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (userReviewText.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Please write a comment",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    val reportRef = db.collection("reports").document(reportId)
                                    val reviewRef = reportRef.collection("reviews").document(reviewerKey)

                                    db.runTransaction { transaction ->
                                        val reportSnapshot = transaction.get(reportRef)

                                        val oldCount = (reportSnapshot.getLong("ratingCount") ?: 0L).toInt()
                                        val oldAverage = reportSnapshot.getDouble("averageRating") ?: 0.0
                                        val currentStatusInDb = reportSnapshot.getString("status") ?: "Pending"

                                        if (currentStatusInDb != "Resolved") {
                                            throw Exception("Reviews are only allowed for resolved reports.")
                                        }

                                        val existingReviewSnapshot = transaction.get(reviewRef)
                                        val alreadyExists = existingReviewSnapshot.exists()

                                        val reviewData = hashMapOf(
                                            "rating" to userRating,
                                            "text" to userReviewText,
                                            "reviewerName" to currentUserName,
                                            "reviewerStudentId" to studentId,
                                            "likes" to if (alreadyExists) ((existingReviewSnapshot.getLong("likes") ?: 0L).toInt()) else 0,
                                            "dislikes" to if (alreadyExists) ((existingReviewSnapshot.getLong("dislikes") ?: 0L).toInt()) else 0,
                                            "adminLiked" to if (alreadyExists) (existingReviewSnapshot.getBoolean("adminLiked") ?: false) else false,
                                            "timestamp" to Timestamp.now()
                                        )

                                        val newCount: Int
                                        val newAverage: Double

                                        if (alreadyExists) {
                                            val oldUserRating = (existingReviewSnapshot.getLong("rating") ?: 0L).toInt()
                                            val totalWithoutOldRating = (oldAverage * oldCount) - oldUserRating
                                            val totalWithNewRating = totalWithoutOldRating + userRating

                                            newCount = oldCount
                                            newAverage = if (newCount > 0) totalWithNewRating / newCount else 0.0
                                        } else {
                                            val totalWithNewRating = (oldAverage * oldCount) + userRating
                                            newCount = oldCount + 1
                                            newAverage = if (newCount > 0) totalWithNewRating / newCount else 0.0
                                        }

                                        transaction.set(reviewRef, reviewData)
                                        transaction.update(
                                            reportRef,
                                            mapOf(
                                                "ratingCount" to newCount,
                                                "averageRating" to newAverage
                                            )
                                        )

                                        alreadyExists
                                    }.addOnSuccessListener { wasUpdated ->
                                        val adminNotification = hashMapOf(
                                            "reportId" to reportId,
                                            "title" to if (wasUpdated) "Report Rating Updated" else "New Report Rating",
                                            "message" to if (wasUpdated) {
                                                "$currentUserName updated their rating for report \"$title\" to $userRating stars."
                                            } else {
                                                "$currentUserName rated report \"$title\" with $userRating stars."
                                            },
                                            "reportTitle" to title,
                                            "status" to selectedStatus,
                                            "reporter" to reporter,
                                            "targetRole" to "Admin",
                                            "targetUser" to "",
                                            "type" to "report_rating",
                                            "read" to false,
                                            "timestamp" to Timestamp.now()
                                        )

                                        db.collection("notifications").add(adminNotification)

                                        Toast.makeText(
                                            context,
                                            if (wasUpdated) "Review updated successfully" else "Review submitted successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.addOnFailureListener { e ->
                                        Toast.makeText(
                                            context,
                                            e.message ?: "Review failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1001B)),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(if (hasExistingUserReview) "Update Review" else "Submit")
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Review Locked",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE1001B)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "You can rate and review this report once its status is marked as Resolved.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ReviewItem(
    review: Review,
    isAdmin: Boolean = false,
    onLikeClick: () -> Unit = {}
) {
    val cardColor = if (review.adminLiked) Color(0xFFFFF8E1) else Color(0xFFF9F9FB)
    val borderModifier = if (review.adminLiked) {
        Modifier.border(1.dp, Color(0xFFFFC107), RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (review.reviewerName.isNotBlank()) {
                Text(
                    text = if (review.reviewerStudentId.isNotBlank()) {
                        "From: ${review.reviewerName} (${review.reviewerStudentId})"
                    } else {
                        "From: ${review.reviewerName}"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(review.rating) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                }

                repeat(5 - review.rating) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "${review.rating}/5",
                    fontSize = 12.sp,
                    color = Color(0xFF444444),
                    fontWeight = FontWeight.Medium
                )

                if (review.adminLiked) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Admin Liked",
                        fontSize = 10.sp,
                        color = Color(0xFFE1001B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFFFDECEC), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = review.text,
                fontSize = 14.sp
            )

            if (review.likes > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Admin likes: ${review.likes}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            if (isAdmin) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onLikeClick() },
                        tint = if (review.adminLiked) Color(0xFFE1001B) else Color.Gray
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = if (review.adminLiked) "Liked by Admin" else "Like review",
                        fontSize = 12.sp,
                        color = if (review.adminLiked) Color(0xFFE1001B) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}