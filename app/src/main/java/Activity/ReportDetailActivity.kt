package Activity

import Activity.ui.theme.SeriousModeTheme
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportUiData(
    val title: String = "",
    val category: String = "",
    val location: String = "",
    val description: String = "",
    val imageUri: Uri? = null,
    val mediaUrl: String = "",
    val mediaType: String = "",
    val status: String = "",
    val dateSubmitted: String = "",
    val reporter: String = "",
    val adminUpdateMediaUrl: String = "",
    val adminUpdateMediaType: String = "",
    val adminNote: String = ""
)

fun loadAccessibilitySettings(context: Context): AccessibilitySettings {
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return AccessibilitySettings(
        textSize = prefs.getString("access_text_size", "Default") ?: "Default",
        boldText = prefs.getBoolean("access_bold_text", false),
        contrastMode = prefs.getString("access_contrast_mode", "Default") ?: "Default",
        reduceMotion = prefs.getBoolean("access_reduce_motion", false),
        grayscaleMode = prefs.getBoolean("access_grayscale_mode", false),
        largeButtons = prefs.getBoolean("access_large_buttons", false),
        simplifiedCards = prefs.getBoolean("access_simplified_cards", false),
        hideMediaPreview = prefs.getBoolean("access_hide_media_preview", false)
    )
}

fun createAdminUpdateImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "ADMIN_UPDATE_${timeStamp}_",
        ".jpg",
        storageDir
    )
}

class ReportDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val role = intent.getStringExtra("role") ?: "Student"
        val reportId = intent.getStringExtra("id")
            ?: intent.getStringExtra("reportId")
            ?: ""

        val initialTitle = intent.getStringExtra("title") ?: ""
        val initialCategory = intent.getStringExtra("category") ?: ""
        val initialLocation = intent.getStringExtra("location") ?: ""
        val initialDescription = intent.getStringExtra("description") ?: ""
        val initialImageUriStr = intent.getStringExtra("imageUri")
        val initialMediaUrl = intent.getStringExtra("mediaUrl") ?: ""
        val initialMediaType = intent.getStringExtra("mediaType") ?: ""
        val initialStatus = intent.getStringExtra("status") ?: ""
        val initialDateSubmitted = intent.getStringExtra("dateSubmitted") ?: ""
        val initialReporter = intent.getStringExtra("reporter") ?: ""

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val fullUserName = prefs.getString("user_name", "Student") ?: "Student"
        val appSettings = loadAccessibilitySettings(this)

        Log.d("ReportDetailActivity", "Received reportId: $reportId")

        val fallbackImageUri =
            if (!initialImageUriStr.isNullOrEmpty()) Uri.parse(initialImageUriStr) else null

        val fallbackReport = ReportUiData(
            title = initialTitle,
            category = initialCategory,
            location = initialLocation,
            description = initialDescription,
            imageUri = fallbackImageUri,
            mediaUrl = initialMediaUrl,
            mediaType = initialMediaType,
            status = initialStatus,
            dateSubmitted = initialDateSubmitted,
            reporter = initialReporter
        )

        setContent {
            SeriousModeTheme {
                ReportDetailScreenHost(
                    onBackClick = { finish() },
                    isAdmin = role.equals("Administrator", ignoreCase = true) ||
                            role.equals("Admin", ignoreCase = true),
                    currentUserName = fullUserName,
                    reportId = reportId,
                    fallbackReport = fallbackReport,
                    settings = appSettings
                )
            }
        }
    }
}

@Composable
fun ReportDetailScreenHost(
    onBackClick: () -> Unit,
    isAdmin: Boolean,
    currentUserName: String,
    reportId: String,
    fallbackReport: ReportUiData,
    settings: AccessibilitySettings
) {
    val db = FirebaseFirestore.getInstance()

    var reportData by remember { mutableStateOf(fallbackReport) }
    var isLoading by remember { mutableStateOf(reportId.isNotBlank() && fallbackReport.title.isBlank()) }

    LaunchedEffect(reportId) {
        if (reportId.isNotBlank()) {
            db.collection("reports").document(reportId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val imageUriStr = doc.getString("imageUrl")
                        reportData = ReportUiData(
                            title = doc.getString("title") ?: fallbackReport.title,
                            category = doc.getString("category") ?: fallbackReport.category,
                            location = doc.getString("location") ?: fallbackReport.location,
                            description = doc.getString("description") ?: fallbackReport.description,
                            imageUri = if (!imageUriStr.isNullOrBlank()) Uri.parse(imageUriStr) else fallbackReport.imageUri,
                            mediaUrl = doc.getString("mediaUrl") ?: fallbackReport.mediaUrl,
                            mediaType = doc.getString("mediaType") ?: fallbackReport.mediaType,
                            status = doc.getString("status") ?: fallbackReport.status,
                            dateSubmitted = doc.getString("dateSubmitted") ?: fallbackReport.dateSubmitted,
                            reporter = doc.getString("reporter") ?: fallbackReport.reporter,
                            adminUpdateMediaUrl = doc.getString("adminUpdateMediaUrl") ?: "",
                            adminUpdateMediaType = doc.getString("adminUpdateMediaType") ?: "",
                            adminNote = doc.getString("adminNote") ?: ""
                        )
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (settings.contrastMode == "Dark Contrast") Color(0xFF121212) else Color.White
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = if (settings.grayscaleMode) Color.DarkGray else Color(0xFFE1001B)
            )
        }
    } else {
        ReportDetailScreen(
            onBackClick = onBackClick,
            title = reportData.title,
            category = reportData.category,
            location = reportData.location,
            description = reportData.description,
            imageUri = reportData.imageUri,
            mediaUrl = reportData.mediaUrl,
            mediaType = reportData.mediaType,
            status = reportData.status,
            dateSubmitted = reportData.dateSubmitted,
            reporter = reportData.reporter,
            adminUpdateMediaUrl = reportData.adminUpdateMediaUrl,
            adminUpdateMediaType = reportData.adminUpdateMediaType,
            adminNote = reportData.adminNote,
            isAdmin = isAdmin,
            currentUserName = currentUserName,
            reportId = reportId,
            settings = settings
        )
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
    adminUpdateMediaUrl: String,
    adminUpdateMediaType: String,
    adminNote: String,
    isAdmin: Boolean = false,
    currentUserName: String,
    reportId: String,
    settings: AccessibilitySettings
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val studentId = prefs.getString("student_id", "") ?: ""

    var selectedStatus by remember { mutableStateOf(status) }
    var pendingStatus by remember { mutableStateOf(status) }
    val statusOptions = listOf("In Review", "In Progress", "Resolved")
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var userReviewText by remember { mutableStateOf("") }
    var userRating by remember { mutableIntStateOf(5) }
    var hasExistingUserReview by remember { mutableStateOf(false) }

    var reportAverageRating by remember { mutableDoubleStateOf(0.0) }
    var reportRatingCount by remember { mutableIntStateOf(0) }

    var adminNoteInput by remember { mutableStateOf(adminNote) }
    var existingAdminNote by remember { mutableStateOf(adminNote) }
    var selectedAdminMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAdminMediaType by remember { mutableStateOf("") }
    var existingAdminUpdateMediaUrl by remember { mutableStateOf(adminUpdateMediaUrl) }
    var existingAdminUpdateMediaType by remember { mutableStateOf(adminUpdateMediaType) }
    var isUpdatingStatus by remember { mutableStateOf(false) }
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val adminCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraImageUri != null) {
            selectedAdminMediaUri = tempCameraImageUri
            selectedAdminMediaType = "image"
        } else {
            tempCameraImageUri = null
            Toast.makeText(context, "Camera capture cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

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

                        val latestStatus = snapshot.getString("status") ?: selectedStatus
                        selectedStatus = latestStatus
                        pendingStatus = latestStatus

                        existingAdminNote = snapshot.getString("adminNote") ?: ""
                        existingAdminUpdateMediaUrl = snapshot.getString("adminUpdateMediaUrl") ?: ""
                        existingAdminUpdateMediaType = snapshot.getString("adminUpdateMediaType") ?: ""
                    }
                }

            db.collection("reports").document(reportId).collection("reviews")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        reviews = snapshot.documents.map { doc ->
                            Review(
                                id = doc.id,
                                rating = (doc.getLong("rating") ?: 5L).toInt(),
                                text = doc.getString("text") ?: "",
                                reviewerName = doc.getString("reviewerName") ?: "",
                                reviewerStudentId = doc.getString("reviewerStudentId") ?: "",
                                likes = (doc.getLong("likes") ?: 0L).toInt(),
                                dislikes = (doc.getLong("dislikes") ?: 0L).toInt(),
                                adminLiked = doc.getBoolean("adminLiked") ?: false
                            )
                        }
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

    val titleSize = when (settings.textSize) {
        "Small" -> 16.sp
        "Large" -> 20.sp
        "Extra Large" -> 22.sp
        else -> 18.sp
    }

    val sectionTitleSize = when (settings.textSize) {
        "Small" -> 14.sp
        "Large" -> 17.sp
        "Extra Large" -> 19.sp
        else -> 15.sp
    }

    val bodySize = when (settings.textSize) {
        "Small" -> 12.sp
        "Large" -> 15.sp
        "Extra Large" -> 17.sp
        else -> 14.sp
    }

    val smallSize = when (settings.textSize) {
        "Small" -> 11.sp
        "Large" -> 13.sp
        "Extra Large" -> 15.sp
        else -> 12.sp
    }

    val screenBg = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color.White
    }

    val cardBg = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF1E1E1E)
        else -> Color.White
    }

    val mutedCardBg = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF2A2A2A)
        else -> Color(0xFFF8F8F8)
    }

    val textColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color.White
        "High Contrast" -> Color.Black
        else -> Color(0xFF222222)
    }

    val secondaryTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFD0D0D0)
        "High Contrast" -> Color(0xFF111111)
        else -> Color.Gray
    }

    val accentColor = when {
        settings.grayscaleMode -> Color(0xFF444444)
        settings.contrastMode == "Dark Contrast" -> Color(0xFFFF6B6B)
        else -> Color(0xFFE1001B)
    }

    val accentSoftColor = when {
        settings.grayscaleMode -> Color(0xFFE4E4E4)
        settings.contrastMode == "Dark Contrast" -> Color(0xFF3A1F24)
        else -> Color(0xFFFDECEC)
    }

    val resolvedColor = when {
        settings.grayscaleMode -> Color(0xFF555555)
        else -> Color(0xFF17B169)
    }

    val inProgressColor = when {
        settings.grayscaleMode -> Color(0xFF777777)
        else -> Color(0xFFFFC107)
    }

    val statusColor = when (selectedStatus) {
        "Resolved" -> resolvedColor
        "In Progress" -> inProgressColor
        else -> accentColor
    }

    val canReview = selectedStatus == "Resolved"
    val cardShape = RoundedCornerShape(if (settings.simplifiedCards) 8.dp else 16.dp)
    val topCardShape = RoundedCornerShape(
        bottomStart = if (settings.simplifiedCards) 10.dp else 20.dp,
        bottomEnd = if (settings.simplifiedCards) 10.dp else 20.dp
    )
    val buttonHeight = if (settings.largeButtons) 52.dp else 44.dp
    val starSize = if (settings.largeButtons) 38.dp else 32.dp
    val mediaHeight = if (settings.largeButtons) 250.dp else 220.dp
    val cardElevation = if (settings.reduceMotion) 0.dp else 2.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(screenBg)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBackClick() }
                .padding(
                    top = if (settings.largeButtons) 14.dp else 10.dp,
                    start = 10.dp,
                    bottom = 8.dp
                )
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Go Back",
                tint = textColor,
                modifier = Modifier.size(if (settings.largeButtons) 24.dp else 20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Back to Reports",
                fontSize = bodySize,
                color = textColor,
                fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal
            )
        }

        if (settings.hideMediaPreview) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(if (settings.contrastMode == "Dark Contrast") Color(0xFF2A2A2A) else Color(0xFFF1F1F1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Media preview hidden",
                    color = secondaryTextColor,
                    fontSize = bodySize,
                    fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal
                )
            }
        } else {
            when {
                mediaType == "image" && mediaUrl.isNotBlank() -> {
                    Image(
                        painter = rememberAsyncImagePainter(mediaUrl),
                        contentDescription = "Report Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(mediaHeight),
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
                            .height(mediaHeight)
                            .background(Color.Black)
                    )
                }

                imageUri != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Report Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(mediaHeight),
                        contentScale = ContentScale.Crop
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(if (settings.contrastMode == "Dark Contrast") Color(0xFF2A2A2A) else Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No media attached",
                            color = secondaryTextColor,
                            fontSize = bodySize
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = topCardShape,
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(cardElevation)
        ) {
            Column(Modifier.padding(if (settings.largeButtons) 20.dp else 18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = titleSize,
                        fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        selectedStatus,
                        color = if (settings.contrastMode == "Dark Contrast") Color.Black else Color.White,
                        fontSize = smallSize,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .background(statusColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = category,
                    color = accentColor,
                    modifier = Modifier
                        .background(accentSoftColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = smallSize,
                    fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(if (settings.largeButtons) 18.dp else 15.dp),
                        tint = secondaryTextColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(location, fontSize = smallSize, color = secondaryTextColor)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(if (settings.largeButtons) 18.dp else 15.dp),
                        tint = secondaryTextColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(dateSubmitted, fontSize = smallSize, color = secondaryTextColor)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(if (settings.largeButtons) 18.dp else 15.dp),
                        tint = secondaryTextColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(reporter, fontSize = smallSize, color = secondaryTextColor)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Description",
                    fontSize = sectionTitleSize,
                    fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                    color = textColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    fontSize = bodySize,
                    color = textColor
                )

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Average Rating",
                        tint = if (settings.grayscaleMode) Color.DarkGray else Color(0xFFFFC107),
                        modifier = Modifier.size(if (settings.largeButtons) 20.dp else 18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (reportRatingCount > 0) {
                            "${String.format("%.1f", reportAverageRating)} / 5"
                        } else {
                            "No ratings yet"
                        },
                        fontSize = bodySize,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (reportRatingCount > 0) textColor else secondaryTextColor
                    )

                    if (reportRatingCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "($reportRatingCount review${if (reportRatingCount > 1) "s" else ""})",
                            fontSize = smallSize,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }

        if (existingAdminUpdateMediaUrl.isNotBlank() || existingAdminNote.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(cardElevation)
            ) {
                Column(Modifier.padding(if (settings.largeButtons) 20.dp else 16.dp)) {
                    Text(
                        text = "Latest Admin Update",
                        fontSize = sectionTitleSize,
                        fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                        color = accentColor
                    )

                    if (existingAdminNote.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = existingAdminNote,
                            fontSize = bodySize,
                            color = textColor
                        )
                    }

                    if (existingAdminUpdateMediaUrl.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        if (existingAdminUpdateMediaType == "image") {
                            Image(
                                painter = rememberAsyncImagePainter(existingAdminUpdateMediaUrl),
                                contentDescription = "Admin update image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else if (existingAdminUpdateMediaType == "video") {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoURI(Uri.parse(existingAdminUpdateMediaUrl))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = false
                                            seekTo(1)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Color.Black)
                            )
                        }
                    }
                }
            }
        }

        if (isAdmin && reportId.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = accentSoftColor),
                elevation = CardDefaults.cardElevation(cardElevation)
            ) {
                Column(Modifier.padding(if (settings.largeButtons) 20.dp else 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(if (settings.largeButtons) 22.dp else 18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Admin Controls",
                            fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = sectionTitleSize,
                            color = accentColor
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Update Report Status",
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium,
                        fontSize = bodySize,
                        color = textColor
                    )

                    ExposedDropdownMenuBox(
                        expanded = statusDropdownExpanded,
                        onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = pendingStatus,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = bodySize
                            ),
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
                                    text = {
                                        Text(
                                            text = option,
                                            color = textColor,
                                            fontSize = bodySize
                                        )
                                    },
                                    onClick = {
                                        pendingStatus = option
                                        statusDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = adminNoteInput,
                        onValueChange = { adminNoteInput = it },
                        label = {
                            Text(
                                "Admin note",
                                fontSize = bodySize
                            )
                        },
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = bodySize
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (settings.largeButtons) 130.dp else 110.dp),
                        maxLines = 4
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Capture proof photo",
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium,
                        fontSize = bodySize,
                        color = textColor
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            try {
                                val imageFile = createAdminUpdateImageFile(context)
                                val imageUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    imageFile
                                )
                                tempCameraImageUri = imageUri
                                adminCameraLauncher.launch(imageUri)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Unable to open camera: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                    ) {
                        Text(
                            text = if (selectedAdminMediaUri != null) "Retake Proof Photo" else "Take Proof Photo",
                            color = if (settings.contrastMode == "Dark Contrast") Color.Black else Color.White,
                            fontSize = bodySize,
                            fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    when {
                        selectedAdminMediaUri != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(selectedAdminMediaUri),
                                contentDescription = "Selected admin proof image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        existingAdminUpdateMediaUrl.isNotBlank() && existingAdminUpdateMediaType == "image" -> {
                            Image(
                                painter = rememberAsyncImagePainter(existingAdminUpdateMediaUrl),
                                contentDescription = "Current admin proof image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "A proof photo is required when moving a report to In Progress or Resolved.",
                        color = accentColor,
                        fontSize = smallSize
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val needsProofPhoto =
                                pendingStatus == "In Progress" || pendingStatus == "Resolved"

                            if (needsProofPhoto &&
                                selectedAdminMediaUri == null &&
                                existingAdminUpdateMediaUrl.isBlank()
                            ) {
                                Toast.makeText(
                                    context,
                                    "Please capture a proof photo before updating the status.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            if (adminNoteInput.trim().isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Please enter an admin note before updating the status.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            isUpdatingStatus = true
                            val cleanNote = adminNoteInput.trim()

                            fun saveReportUpdate(mediaUrlToSave: String, mediaTypeToSave: String) {
                                val updates = hashMapOf<String, Any>(
                                    "status" to pendingStatus,
                                    "resolvedTimestamp" to if (pendingStatus == "Resolved") System.currentTimeMillis() else 0L,
                                    "adminNote" to cleanNote,
                                    "lastUpdatedByAdmin" to currentUserName,
                                    "lastUpdatedAt" to Timestamp.now(),
                                    "adminUpdateMediaUrl" to mediaUrlToSave,
                                    "adminUpdateMediaType" to mediaTypeToSave
                                )

                                db.collection("reports").document(reportId)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        existingAdminUpdateMediaUrl = mediaUrlToSave
                                        existingAdminUpdateMediaType = mediaTypeToSave
                                        existingAdminNote = cleanNote
                                        selectedStatus = pendingStatus
                                        selectedAdminMediaUri = null
                                        selectedAdminMediaType = ""
                                        tempCameraImageUri = null
                                        adminNoteInput = cleanNote
                                        isUpdatingStatus = false

                                        val studentTitle = when (pendingStatus) {
                                            "Resolved" -> "Report Resolved"
                                            "In Progress" -> "Report Update"
                                            else -> "Report Status Updated"
                                        }

                                        val studentMessage =
                                            "Your report \"$title\" is now marked as $pendingStatus. Admin note: $cleanNote"

                                        val studentNotification = hashMapOf(
                                            "reportId" to reportId,
                                            "title" to studentTitle,
                                            "message" to studentMessage,
                                            "reportTitle" to title,
                                            "status" to pendingStatus,
                                            "reporter" to reporter,
                                            "targetRole" to "Student",
                                            "targetUser" to reporter,
                                            "type" to "report_status",
                                            "read" to false,
                                            "timestamp" to Timestamp.now(),
                                            "adminNote" to cleanNote,
                                            "adminUpdateMediaUrl" to mediaUrlToSave,
                                            "adminUpdateMediaType" to mediaTypeToSave
                                        )

                                        val adminNotification = hashMapOf(
                                            "reportId" to reportId,
                                            "title" to "Report Status Changed",
                                            "message" to "A report \"$title\" is now marked as $pendingStatus.",
                                            "reportTitle" to title,
                                            "status" to pendingStatus,
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

                                        Toast.makeText(
                                            context,
                                            "Status updated to $pendingStatus",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isUpdatingStatus = false
                                        Toast.makeText(
                                            context,
                                            "Failed to update status: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }

                            if (selectedAdminMediaUri != null) {
                                val storageRef = storage.reference.child(
                                    "report_admin_updates/$reportId/${System.currentTimeMillis()}.jpg"
                                )

                                storageRef.putFile(selectedAdminMediaUri!!)
                                    .continueWithTask { task ->
                                        if (!task.isSuccessful) {
                                            throw task.exception ?: Exception("Upload failed")
                                        }
                                        storageRef.downloadUrl
                                    }
                                    .addOnSuccessListener { downloadUri ->
                                        saveReportUpdate(downloadUri.toString(), "image")
                                    }
                                    .addOnFailureListener { e ->
                                        isUpdatingStatus = false
                                        Toast.makeText(
                                            context,
                                            "Photo upload failed: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                saveReportUpdate(
                                    existingAdminUpdateMediaUrl,
                                    existingAdminUpdateMediaType.ifBlank { "image" }
                                )
                            }
                        },
                        enabled = !isUpdatingStatus,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                    ) {
                        Text(
                            if (isUpdatingStatus) "Updating..." else "Update Status",
                            fontSize = bodySize,
                            color = if (settings.contrastMode == "Dark Contrast") Color.Black else Color.White,
                            fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium
                        )
                    }

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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.grayscaleMode) Color.DarkGray else Color.Red
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Delete Resolved Report",
                                fontSize = bodySize,
                                fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Reviews",
                fontSize = titleSize,
                fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isAdmin || canReview) {
                if (reviews.isEmpty()) {
                    Text(
                        "No reviews yet.",
                        fontSize = bodySize,
                        color = secondaryTextColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    reviews.forEach { review ->
                        ReviewItem(
                            review = review,
                            isAdmin = isAdmin,
                            settings = settings,
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
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = mutedCardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Reviews Locked",
                            fontSize = sectionTitleSize,
                            fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                            color = accentColor
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Reviews will be available once this report is marked as Resolved.",
                            fontSize = bodySize,
                            color = secondaryTextColor,
                            lineHeight = (bodySize.value + 6).sp
                        )
                    }
                }
            }

            if (!isAdmin && reportId.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                if (canReview) {
                    Card(
                        shape = cardShape,
                        elevation = CardDefaults.cardElevation(cardElevation),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = if (hasExistingUserReview) "Update Your Review" else "Leave a Review",
                                fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = sectionTitleSize,
                                color = textColor
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { i ->
                                    Icon(
                                        imageVector = if (i <= userRating) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = "Star $i",
                                        tint = if (settings.grayscaleMode) Color.DarkGray else Color(0xFFFFC107),
                                        modifier = Modifier
                                            .size(starSize)
                                            .clickable { userRating = i }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = userReviewText,
                                onValueChange = { userReviewText = it },
                                placeholder = {
                                    Text(
                                        "Write your feedback...",
                                        fontSize = bodySize
                                    )
                                },
                                textStyle = TextStyle(
                                    color = textColor,
                                    fontSize = bodySize
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (settings.largeButtons) 120.dp else 100.dp),
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
                                        val currentStatusInDb = reportSnapshot.getString("status") ?: "In Review"

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
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .height(buttonHeight)
                            ) {
                                Text(
                                    if (hasExistingUserReview) "Update Review" else "Submit",
                                    fontSize = bodySize,
                                    color = if (settings.contrastMode == "Dark Contrast") Color.Black else Color.White,
                                    fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = mutedCardBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Review Locked",
                                fontSize = sectionTitleSize,
                                fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                                color = accentColor
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "You can rate and review this report once its status is marked as Resolved.",
                                fontSize = bodySize,
                                color = secondaryTextColor,
                                lineHeight = (bodySize.value + 6).sp
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
    settings: AccessibilitySettings,
    onLikeClick: () -> Unit = {}
) {
    val titleSize = when (settings.textSize) {
        "Small" -> 11.sp
        "Large" -> 13.sp
        "Extra Large" -> 15.sp
        else -> 12.sp
    }

    val bodySize = when (settings.textSize) {
        "Small" -> 13.sp
        "Large" -> 15.sp
        "Extra Large" -> 17.sp
        else -> 14.sp
    }

    val smallSize = when (settings.textSize) {
        "Small" -> 10.sp
        "Large" -> 12.sp
        "Extra Large" -> 14.sp
        else -> 11.sp
    }

    val textColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color.White
        else -> Color(0xFF222222)
    }

    val secondaryTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFD0D0D0)
        else -> Color.Gray
    }

    val accentColor = when {
        settings.grayscaleMode -> Color(0xFF444444)
        settings.contrastMode == "Dark Contrast" -> Color(0xFFFF6B6B)
        else -> Color(0xFFE1001B)
    }

    val starColor = if (settings.grayscaleMode) Color.DarkGray else Color(0xFFFFC107)

    val cardColor = if (review.adminLiked) {
        if (settings.grayscaleMode) Color(0xFFEAEAEA) else Color(0xFFFFF8E1)
    } else {
        if (settings.contrastMode == "Dark Contrast") Color(0xFF1E1E1E) else Color(0xFFF9F9FB)
    }

    val borderModifier = if (review.adminLiked) {
        Modifier.border(
            1.dp,
            if (settings.grayscaleMode) Color.DarkGray else Color(0xFFFFC107),
            RoundedCornerShape(if (settings.simplifiedCards) 8.dp else 12.dp)
        )
    } else {
        Modifier
    }

    Card(
        shape = RoundedCornerShape(if (settings.simplifiedCards) 8.dp else 12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
    ) {
        Column(Modifier.padding(if (settings.largeButtons) 16.dp else 12.dp)) {
            if (review.reviewerName.isNotBlank()) {
                Text(
                    text = if (review.reviewerStudentId.isNotBlank()) {
                        "From: ${review.reviewerName} (${review.reviewerStudentId})"
                    } else {
                        "From: ${review.reviewerName}"
                    },
                    fontSize = titleSize,
                    color = secondaryTextColor
                )
                Spacer(Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(review.rating) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = starColor,
                        modifier = Modifier.size(if (settings.largeButtons) 18.dp else 16.dp)
                    )
                }

                repeat(5 - review.rating) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = secondaryTextColor,
                        modifier = Modifier.size(if (settings.largeButtons) 18.dp else 16.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "${review.rating}/5",
                    fontSize = titleSize,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )

                if (review.adminLiked) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Admin Liked",
                        fontSize = smallSize,
                        color = accentColor,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .background(
                                if (settings.grayscaleMode) Color(0xFFE0E0E0) else Color(0xFFFDECEC),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = review.text,
                fontSize = bodySize,
                color = textColor
            )

            if (review.likes > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Admin likes: ${review.likes}",
                    fontSize = smallSize,
                    color = secondaryTextColor
                )
            }

            if (isAdmin) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        modifier = Modifier
                            .size(if (settings.largeButtons) 20.dp else 16.dp)
                            .clickable(onClick = onLikeClick),
                        tint = if (review.adminLiked) accentColor else secondaryTextColor
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (review.adminLiked) "Liked by Admin" else "Like review",
                        fontSize = titleSize,
                        color = if (review.adminLiked) accentColor else secondaryTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}