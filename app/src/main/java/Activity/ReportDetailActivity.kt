package Activity

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import Activity.ui.theme.SeriousModeTheme
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.border

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
        val reportId = intent.getStringExtra("id") ?: ""
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
                    status = status,
                    dateSubmitted = dateSubmitted,
                    reporter = reporter,
                    isAdmin = role == "Administrator",
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
    status: String,
    dateSubmitted: String,
    reporter: String,
    isAdmin: Boolean = false,
    reportId: String
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var selectedStatus by remember { mutableStateOf(status) }
    val statusOptions = listOf("Pending", "In Progress", "Resolved")
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    // Reviews State
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var userReviewText by remember { mutableStateOf("") }
    var userRating by remember { mutableIntStateOf(5) }

    // Load Reviews
    LaunchedEffect(reportId) {
        if (reportId.isNotEmpty()) {
            db.collection("reports").document(reportId).collection("reviews")
                .addSnapshotListener { snapshot, e ->
                    if (snapshot != null) {
                        val loadedReviews = snapshot.documents.map { doc ->
                            Review(
                                id = doc.id,
                                rating = (doc.getLong("rating") ?: 5).toInt(),
                                text = doc.getString("text") ?: "",
                                likes = (doc.getLong("likes") ?: 0).toInt(),
                                dislikes = (doc.getLong("dislikes") ?: 0).toInt(),
                                adminLiked = doc.getBoolean("adminLiked") ?: false
                            )
                        }
                        reviews = loadedReviews
                    }
                }
        }
    }

    // Check if the report should be auto-deleted (2 hours after resolution)
    LaunchedEffect(selectedStatus, reportId) {
        if (selectedStatus == "Resolved" && reportId.isNotEmpty()) {
            db.collection("reports").document(reportId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val resolvedTimestamp = document.getLong("resolvedTimestamp") ?: 0L
                        if (resolvedTimestamp > 0) {
                            val currentTime = System.currentTimeMillis()
                            val twoHoursInMillis = 2 * 60 * 60 * 1000 // 2 hours
                            if (currentTime - resolvedTimestamp > twoHoursInMillis) {
                                db.collection("reports").document(reportId).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Resolved report auto-deleted", Toast.LENGTH_LONG).show()
                                        onBackClick()
                                    }
                            }
                        }
                    }
                }
        }
    }

    // Color badge based on current status
    val statusColor = when(selectedStatus) {
        "Resolved" -> Color(0xFF17B169)
        "In Progress" -> Color(0xFFFFC107)
        else -> Color(0xFFE1001B)
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
                                        if (selectedStatus != option) {
                                            selectedStatus = option
                                            statusDropdownExpanded = false
                                            
                                            val updates = hashMapOf<String, Any>("status" to option)
                                            if (option == "Resolved") {
                                                updates["resolvedTimestamp"] = System.currentTimeMillis()
                                            } else {
                                                updates["resolvedTimestamp"] = 0L
                                            }

                                            db.collection("reports").document(reportId)
                                                .update(updates)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
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

                    // Delete Button (Only for Resolved)
                    if (selectedStatus == "Resolved") {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                db.collection("reports").document(reportId).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Report Deleted", Toast.LENGTH_SHORT).show()
                                        onBackClick()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to delete report", Toast.LENGTH_SHORT).show()
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

        // Reviews Section
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Reviews", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            if (reviews.isEmpty()) {
                Text("No reviews yet.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            } else {
                reviews.forEach { review ->
                    ReviewItem(
                        review = review,
                        isAdmin = isAdmin,
                        onLikeClick = {
                            // When admin likes, toggle the adminLiked status in Firestore
                            if (isAdmin && reportId.isNotEmpty() && review.id.isNotEmpty()) {
                                db.collection("reports").document(reportId)
                                    .collection("reviews").document(review.id)
                                    .update("adminLiked", !review.adminLiked)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Add Review Area
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Leave a Review", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            if (userReviewText.isNotBlank()) {
                                val reviewData = hashMapOf(
                                    "rating" to userRating,
                                    "text" to userReviewText,
                                    "likes" to 0,
                                    "dislikes" to 0,
                                    "adminLiked" to false,
                                    "timestamp" to com.google.firebase.Timestamp.now()
                                )
                                db.collection("reports").document(reportId).collection("reviews")
                                    .add(reviewData)
                                    .addOnSuccessListener {
                                        userReviewText = ""
                                        userRating = 5
                                        Toast.makeText(context, "Review submitted", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(context, "Please write a comment", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1001B)),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ReviewItem(review: Review, isAdmin: Boolean = false, onLikeClick: () -> Unit = {}) {
    // Highlight if admin liked
    val cardColor = if (review.adminLiked) Color(0xFFFFF8E1) else Color(0xFFF9F9FB) // Slight yellow highlight if liked
    val borderModifier = if (review.adminLiked) Modifier.border(1.dp, Color(0xFFFFC107), RoundedCornerShape(8.dp)) else Modifier

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth().then(borderModifier)
    ) {
        Column(Modifier.padding(12.dp)) {
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
                if (review.adminLiked) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Admin Liked",
                        fontSize = 10.sp,
                        color = Color(0xFFE1001B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Color(0xFFFDECEC), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(review.text, fontSize = 14.sp)
            
            // Likes/Dislikes
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // If admin, this like button toggles adminLiked status
                // If regular user, it's just a display for now (or regular like)
                Icon(
                    imageVector = if(review.adminLiked) Icons.Outlined.ThumbUp else Icons.Outlined.ThumbUp, 
                    contentDescription = "Like", 
                    modifier = Modifier.size(14.dp).clickable { if (isAdmin) onLikeClick() }, // Admin can click to toggle "Admin Liked"
                    tint = if (review.adminLiked) Color(0xFFE1001B) else Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isAdmin) "Admin Like" else "${review.likes}", // Show text hint for admin
                    fontSize = 12.sp, 
                    color = if (review.adminLiked) Color(0xFFE1001B) else Color.Gray
                )
                
                if (!isAdmin) {
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Outlined.ThumbDown, contentDescription = "Dislike", modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text("${review.dislikes}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
