package Activity

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import Activity.ui.theme.SeriousModeTheme
import Activity.ReportRepository
import Activity.Review
import Activity.ReviewRepository

class ReportDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = intent.getStringExtra("role") ?: "Student"
        val title = intent.getStringExtra("title") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val location = intent.getStringExtra("location") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val imageUriStr = intent.getStringExtra("imageUri")
        val status = intent.getStringExtra("status") ?: ""
        val dateSubmitted = intent.getStringExtra("dateSubmitted") ?: ""
        val reporter = intent.getStringExtra("reporter") ?: ""
        val imageUri = if (!imageUriStr.isNullOrEmpty()) Uri.parse(imageUriStr) else null

        // Get live reference to the report instance
        val report = ReportRepository.reports.find { it.title == title }

        setContent {
            SeriousModeTheme {
                ReportDetailScreen(
                    onBackClick = { finish() },
                    title = title,
                    category = category,
                    location = location,
                    description = description,
                    imageUri = imageUri,
                    report = report,
                    status = status,
                    dateSubmitted = dateSubmitted,
                    reporter = reporter,
                    isAdmin = role == "Administrator"
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
    report: Report?, // Reference to updatable report object
    status: String,
    dateSubmitted: String,
    reporter: String,
    isAdmin: Boolean = false
) {
    var newRating by remember { mutableStateOf(1) }
    var newReviewText by remember { mutableStateOf("") }
    val reviews = remember { mutableStateListOf<Review>() }
    var selectedStatus by remember { mutableStateOf(report?.status ?: status) }
    val statusOptions = listOf("Pending", "In Progress", "Resolved")
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    // Color badge based on current status
    val statusColor = when(selectedStatus) {
        "Resolved" -> Color(0xFF17B169)
        "In Progress" -> Color(0xFFFFC107)
        else -> Color(0xFFE1001B)
    }

    // Keep selectedStatus synced to the report object's status
    LaunchedEffect(report?.status) {
        if (report?.status != selectedStatus) {
            selectedStatus = report?.status ?: status
        }
    }

    LaunchedEffect(title) {
        val sharedList = ReviewRepository.reviewsByReport.getOrPut(title) { mutableListOf() }
        reviews.clear()
        reviews.addAll(sharedList)
    }

    fun addReview(review: Review) {
        val sharedList = ReviewRepository.reviewsByReport.getOrPut(title) { mutableListOf() }
        sharedList.add(review)
        reviews.add(review)
    }

    fun likeReview(index: Int) {
        val sharedList = ReviewRepository.reviewsByReport[title]
        sharedList?.get(index)?.likes = sharedList?.get(index)?.likes?.plus(1) ?: 1
        reviews[index] = reviews[index].copy(likes = reviews[index].likes + 1)
    }

    fun dislikeReview(index: Int) {
        val sharedList = ReviewRepository.reviewsByReport[title]
        sharedList?.get(index)?.dislikes = sharedList?.get(index)?.dislikes?.plus(1) ?: 1
        reviews[index] = reviews[index].copy(dislikes = reviews[index].dislikes + 1)
    }

    fun adminLikeReview(index: Int) {
        val sharedList = ReviewRepository.reviewsByReport[title]
        sharedList?.get(index)?.adminLiked = true
        reviews[index] = reviews[index].copy(adminLiked = true)
    }

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
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = "Report Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp),
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
            }
        }

        if (isAdmin && report != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEC))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color(0xFFE1001B)
                        )
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
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
                                        selectedStatus = option
                                        statusDropdownExpanded = false
                                        report.status = option
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        "Change the status to keep everyone informed about the progress",
                        color = Color(0xFFE1001B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp, start = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Reviews (${reviews.size})", fontWeight = FontWeight.Bold)
                if (reviews.isEmpty()) {
                    Text(
                        "No reviews yet",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else {
                    reviews.forEachIndexed { reviewIndex, review ->
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            ) {
                                for (i in 1..5) {
                                    Text(
                                        text = if (i <= review.rating) "★" else "☆",
                                        color = if (i <= review.rating) Color(0xFFFFC107) else Color.Gray,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(review.text, fontSize = 13.sp)
                                if (isAdmin) {
                                    if (!review.adminLiked) {
                                        Icon(
                                            imageVector = Icons.Filled.ThumbUp,
                                            contentDescription = "Admin Like",
                                            tint = Color(0xFFE1001B),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clickable { adminLikeReview(reviewIndex) }
                                                .padding(horizontal = 6.dp)
                                        )
                                    } else {
                                        Text(
                                            "admin liked your review",
                                            color = Color(0xFFE1001B),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.width(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ThumbUp,
                                            contentDescription = "Like review",
                                            tint = if (review.likes > 0) Color(0xFFE1001B) else Color.Gray,
                                            modifier = Modifier.size(20.dp).clickable { likeReview(reviewIndex) }
                                        )
                                        Text("${review.likes}", fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.ThumbDown,
                                            contentDescription = "Dislike review",
                                            tint = if (review.dislikes > 0) Color(0xFFE1001B) else Color.Gray,
                                            modifier = Modifier.size(20.dp).clickable { dislikeReview(reviewIndex) }
                                        )
                                        Text("${review.dislikes}", fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }
                            if (review.adminLiked && !isAdmin) {
                                Text(
                                    "admin liked your review",
                                    color = Color(0xFFE1001B),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
                Divider(Modifier.padding(vertical = 12.dp))

                Text("Add Your Review", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Rating:")
                    Spacer(Modifier.width(6.dp))
                    for (i in 1..5) {
                        Text(
                            text = if (i <= newRating) "★" else "☆",
                            fontSize = 24.sp,
                            color = if (i <= newRating) Color(0xFFFFC107) else Color.Gray,
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .clickable { newRating = i }
                        )
                    }
                }
                OutlinedTextField(
                    value = newReviewText,
                    onValueChange = { newReviewText = it },
                    placeholder = { Text("Share your thoughts about this report...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
                Button(
                    onClick = {
                        if (newReviewText.isNotBlank()) {
                            addReview(Review(newRating, newReviewText))
                            newReviewText = ""
                            newRating = 1
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1001B))
                ) {
                    Text("Submit Review", color = Color.White)
                }
            }
        }
    }
}
